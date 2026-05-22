package com.agenciahub.api.application.usecases.solicitacao.pub.consent;

import com.agenciahub.api.application.persistence.entity.ConsentimentoLog;
import com.agenciahub.api.application.persistence.entity.SolicitacaoSubmission;
import com.agenciahub.api.application.persistence.repository.ConsentimentoLogRepository;
import com.agenciahub.api.application.persistence.repository.SolicitacaoSubmissionRepository;
import com.agenciahub.api.exception.ConsentRevocationException;
import com.agenciahub.api.validation.PhoneValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RevokeConsent implements RevokeConsentUseCase {

    private final SolicitacaoSubmissionRepository submissionRepository;
    private final ConsentimentoLogRepository consentimentoLogRepository;

    @Override
    @Transactional
    public void execute(RevokeConsentRequestDTO request) {
        String telefoneDigits = PhoneValidator.normalize(request.telefone());
        List<SolicitacaoSubmission> submissions = submissionRepository
                .findByEmailAndTelefone(request.email().trim(), telefoneDigits);

        if (submissions.isEmpty()) {
            throw new ConsentRevocationException("nenhuma submissão encontrada para os dados informados.");
        }

        String ip = extractClientIp();

        for (SolicitacaoSubmission sub : submissions) {
            sub.setConsentimentoLgpd(false);
            submissionRepository.save(sub);

            consentimentoLogRepository.save(ConsentimentoLog.builder()
                    .submission(sub)
                    .tipo("REVOGACAO")
                    .ip(ip)
                    .build());
        }
    }

    private String extractClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
