package com.agenciahub.api.service;

import com.agenciahub.api.domain.VerificationCodeType;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.entity.VerificationCode;
import com.agenciahub.api.repository.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Service responsible for generating, storing, and verifying 6-digit verification codes.
 * Codes are stored as BCrypt hashes and expire after 15 minutes.
 */
@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeService.class);

    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRATION_MINUTES = 15;
    private static final int MAX_ATTEMPTS_PER_CODE = 5;
    private static final int MAX_CODES_PER_HOUR = 5;

    private final VerificationCodeRepository verificationCodeRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a 6-digit code, hashes it, stores in DB, invalidates previous codes,
     * and sends via email.
     *
     * @param email    recipient email
     * @param type     the type of verification (EMAIL_VERIFICATION or PASSWORD_RESET)
     * @param user     the user entity (may be null for password reset when user not yet loaded)
     * @param userName display name for the email
     * @return true if code was generated and sent, false if rate limited
     */
    @Transactional
    public boolean generateAndSend(String email, VerificationCodeType type, User user, String userName) {
        // Rate limiting: max 5 codes per hour per email
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentCount = verificationCodeRepository.countByEmailAndCreatedAtAfter(email, oneHourAgo);
        if (recentCount >= MAX_CODES_PER_HOUR) {
            log.warn("Rate limit reached for email {} — {} codes in the last hour", email, recentCount);
            return false;
        }

        // Invalidate all previous codes for this email and type
        verificationCodeRepository.invalidateAllByEmailAndType(email, type);

        // Generate 6-digit code
        String code = generateCode();
        String codeHash = passwordEncoder.encode(code);

        // Store in DB
        VerificationCode verificationCode = VerificationCode.builder()
                .user(user)
                .email(email)
                .codeHash(codeHash)
                .type(type)
                .attempts(0)
                .used(false)
                .expiresAt(Instant.now().plus(CODE_EXPIRATION_MINUTES, ChronoUnit.MINUTES))
                .build();

        verificationCodeRepository.save(verificationCode);

        // Send via email
        if (type == VerificationCodeType.EMAIL_VERIFICATION) {
            emailService.sendVerificationCode(email, code, userName);
        } else if (type == VerificationCodeType.PASSWORD_RESET) {
            emailService.sendPasswordResetCode(email, code, userName);
        }

        log.debug("Verification code generated and sent for email={}, type={}", email, type);
        return true;
    }

    /**
     * Verifies a code against the stored hash, checking expiration and attempt limits.
     *
     * @param email the email associated with the code
     * @param code  the plain-text code to verify
     * @param type  the type of verification
     * @return true if the code is valid, not expired, and within attempt limits
     */
    @Transactional
    public boolean verify(String email, String code, VerificationCodeType type) {
        Optional<VerificationCode> optCode = verificationCodeRepository
                .findFirstByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(email, type);

        if (optCode.isEmpty()) {
            return false;
        }

        VerificationCode verificationCode = optCode.get();

        // Check expiration
        if (Instant.now().isAfter(verificationCode.getExpiresAt())) {
            log.debug("Code expired for email={}, type={}", email, type);
            return false;
        }

        // Check max attempts
        if (verificationCode.getAttempts() >= MAX_ATTEMPTS_PER_CODE) {
            log.debug("Max attempts reached for email={}, type={}", email, type);
            verificationCode.setUsed(true);
            verificationCodeRepository.save(verificationCode);
            return false;
        }

        // Increment attempts
        verificationCode.setAttempts(verificationCode.getAttempts() + 1);

        // Verify code against hash
        if (passwordEncoder.matches(code, verificationCode.getCodeHash())) {
            verificationCode.setUsed(true);
            verificationCodeRepository.save(verificationCode);
            return true;
        }

        // Wrong code — save incremented attempts
        verificationCodeRepository.save(verificationCode);

        // If max attempts reached after this failure, mark as used
        if (verificationCode.getAttempts() >= MAX_ATTEMPTS_PER_CODE) {
            verificationCode.setUsed(true);
            verificationCodeRepository.save(verificationCode);
        }

        return false;
    }

    /**
     * Invalidates all previous codes for the given email and type.
     *
     * @param email the email to invalidate codes for
     * @param type  the type of verification codes to invalidate
     */
    @Transactional
    public void invalidatePrevious(String email, VerificationCodeType type) {
        verificationCodeRepository.invalidateAllByEmailAndType(email, type);
    }

    /**
     * Generates a random 6-digit numeric code.
     */
    private String generateCode() {
        int code = secureRandom.nextInt(900_000) + 100_000; // 100000–999999
        return String.valueOf(code);
    }
}
