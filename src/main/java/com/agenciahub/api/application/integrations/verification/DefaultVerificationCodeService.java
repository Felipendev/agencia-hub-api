package com.agenciahub.api.application.integrations.verification;

import com.agenciahub.api.application.integrations.email.EmailService;
import com.agenciahub.api.domain.VerificationCodeType;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.entity.VerificationCode;
import com.agenciahub.api.application.persistence.repository.VerificationCodeRepository;
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

@Service
@RequiredArgsConstructor
public class DefaultVerificationCodeService implements VerificationCodePort {

    private static final Logger log = LoggerFactory.getLogger(DefaultVerificationCodeService.class);

    private static final int CODE_EXPIRATION_MINUTES = 15;
    private static final int MAX_ATTEMPTS_PER_CODE = 5;
    private static final int MAX_CODES_PER_HOUR = 5;

    private final VerificationCodeRepository verificationCodeRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public boolean generateAndSend(String email, VerificationCodeType type, PlatformAccount user, String userName) {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentCount = verificationCodeRepository.countByEmailAndCreatedAtAfter(email, oneHourAgo);
        if (recentCount >= MAX_CODES_PER_HOUR) {
            log.warn("Rate limit reached for email {} — {} codes in the last hour", email, recentCount);
            return false;
        }

        verificationCodeRepository.invalidateAllByEmailAndType(email, type);

        String code = generateCode();
        String codeHash = passwordEncoder.encode(code);

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

        if (type == VerificationCodeType.EMAIL_VERIFICATION) {
            emailService.sendVerificationCode(email, code, userName);
        } else if (type == VerificationCodeType.PASSWORD_RESET) {
            emailService.sendPasswordResetCode(email, code, userName);
        }

        log.debug("Verification code generated and sent for email={}, type={}", email, type);
        return true;
    }

    @Override
    @Transactional
    public boolean verify(String email, String code, VerificationCodeType type) {
        Optional<VerificationCode> optCode = verificationCodeRepository
                .findFirstByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(email, type);

        if (optCode.isEmpty()) {
            return false;
        }

        VerificationCode verificationCode = optCode.get();

        if (Instant.now().isAfter(verificationCode.getExpiresAt())) {
            log.debug("Code expired for email={}, type={}", email, type);
            return false;
        }

        if (verificationCode.getAttempts() >= MAX_ATTEMPTS_PER_CODE) {
            log.debug("Max attempts reached for email={}, type={}", email, type);
            verificationCode.setUsed(true);
            verificationCodeRepository.save(verificationCode);
            return false;
        }

        verificationCode.setAttempts(verificationCode.getAttempts() + 1);

        if (passwordEncoder.matches(code, verificationCode.getCodeHash())) {
            verificationCode.setUsed(true);
            verificationCodeRepository.save(verificationCode);
            return true;
        }

        verificationCodeRepository.save(verificationCode);

        if (verificationCode.getAttempts() >= MAX_ATTEMPTS_PER_CODE) {
            verificationCode.setUsed(true);
            verificationCodeRepository.save(verificationCode);
        }

        return false;
    }

    @Override
    @Transactional
    public void invalidatePrevious(String email, VerificationCodeType type) {
        verificationCodeRepository.invalidateAllByEmailAndType(email, type);
    }

    private String generateCode() {
        int code = secureRandom.nextInt(900_000) + 100_000;
        return String.valueOf(code);
    }
}
