package com.agenciahub.api.application.usecases.solicitacao.pub.submit;

import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.application.usecases.solicitacao.pub.submit.PublicSolicitacaoSubmitRequestDTO;
import com.agenciahub.api.application.usecases.solicitacao.pub.submit.PublicSolicitacaoSubmitResponseDTO;
import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.SolicitacaoConfig;
import com.agenciahub.api.application.persistence.entity.SolicitacaoSubmission;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.repository.SolicitacaoConfigRepository;
import com.agenciahub.api.application.persistence.repository.SolicitacaoSubmissionRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.application.integrations.email.EmailService;
import com.agenciahub.api.validation.PhoneValidator;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubmitPublicSolicitacao implements SubmitPublicSolicitacaoUseCase {

    private final SolicitacaoSubmissionRepository submissionRepository;
    private final SolicitacaoConfigRepository configRepository;
    private final PlatformAccountRepository userRepository;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:3000}")
    private String appBaseUrl;

    @Override
    public PublicSolicitacaoSubmitResponseDTO execute(PublicSolicitacaoSubmitRequestDTO request) {
        String telefoneDigits = PhoneValidator.normalize(request.telefone());
        if (!PhoneValidator.isValid(telefoneDigits)) {
            throw new IllegalArgumentException("informe um celular válido com DDD (10 ou 11 dígitos).");
        }

        JsonNode detalhes = request.detalhes();
        if (!hasRouteInfo(detalhes)) {
            throw new IllegalArgumentException("informe origem e/ou destino.");
        }

        var configOpt = configRepository.findFirstBySlug(request.slug().trim());
        Agency agency = configOpt.map(SolicitacaoConfig::getAgency).orElse(null);
        PlatformAccount referral = resolveReferral(request, agency);

        var submission = SolicitacaoSubmission.builder()
                .agency(agency)
                .referralSeller(referral)
                .slug(request.slug().trim())
                .nome(request.nome().trim())
                .email(request.email() != null ? request.email().trim() : "")
                .telefone(telefoneDigits)
                .observacoes(request.observacoes() != null ? request.observacoes().trim() : "")
                .detalhes(detalhes)
                .consentimentoLgpd(Boolean.TRUE.equals(request.consentimentoLgpd()))
                .build();

        submission = submissionRepository.save(submission);
        dispatchAlertEmail(agency, submission);
        return new PublicSolicitacaoSubmitResponseDTO(true, submission.getId());
    }

    private void dispatchAlertEmail(Agency agency, SolicitacaoSubmission submission) {
        if (agency == null) return;
        String recipientEmail = resolveAlertRecipient(agency);
        if (recipientEmail == null || recipientEmail.isBlank()) return;

        JsonNode det = submission.getDetalhes();
        String rota = buildRota(det);
        String datas = buildDatas(det);
        String dashboardUrl = appBaseUrl + "/cotacoes";

        emailService.sendNewSubmissionAlert(
                recipientEmail,
                agency.getName(),
                submission.getNome(),
                submission.getTelefone(),
                rota,
                datas,
                dashboardUrl);
    }

    private String resolveAlertRecipient(Agency agency) {
        if (agency.getCommercialEmail() != null && !agency.getCommercialEmail().isBlank()) {
            return agency.getCommercialEmail();
        }
        return userRepository
                .findByAgency_IdAndAccountKindAndActiveTrue(agency.getId(), AccountKind.AGENCY_OWNER)
                .stream()
                .findFirst()
                .map(PlatformAccount::getEmail)
                .orElse(null);
    }

    private static String buildRota(JsonNode det) {
        if (det == null) return "—";
        String origem = text(det, "origem");
        String destino = text(det, "destinoForm");
        if (destino == null || destino.isBlank()) {
            JsonNode arr = det.get("destinosTrechos");
            if (arr != null && arr.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode n : arr) {
                    if (n != null && n.isTextual() && !n.asText().isBlank()) {
                        if (!sb.isEmpty()) sb.append(" · ");
                        sb.append(n.asText().trim());
                    }
                }
                if (!sb.isEmpty()) destino = sb.toString();
            }
        }
        if (origem != null && !origem.isBlank() && destino != null && !destino.isBlank())
            return origem + " → " + destino;
        if (origem != null && !origem.isBlank()) return origem;
        if (destino != null && !destino.isBlank()) return destino;
        return "—";
    }

    private static String buildDatas(JsonNode det) {
        if (det == null) return "—";
        String ida = text(det, "dataIda");
        String volta = text(det, "dataVolta");
        if (ida != null && !ida.isBlank() && volta != null && !volta.isBlank())
            return ida + " → " + volta;
        if (ida != null && !ida.isBlank()) return "Ida: " + ida;
        return "—";
    }

    private PlatformAccount resolveReferral(PublicSolicitacaoSubmitRequestDTO request, Agency agency) {
        String code = request.sellerPublicCode() != null ? request.sellerPublicCode().trim() : "";
        if (!code.isEmpty()) {
            return resolveReferralByPublicCode(code, agency);
        }
        return resolveReferralSeller(request.referralSellerId(), agency);
    }

    private PlatformAccount resolveReferralByPublicCode(String code, Agency agency) {
        if (agency == null) {
            throw new IllegalArgumentException(
                    "não é possível atribuir vendedor sem configuração de agência para este link.");
        }
        PlatformAccount u = userRepository
                .findByPublicLinkCode(code)
                .orElseThrow(() -> new IllegalArgumentException("código de vendedor inválido ou inexistente."));
        if (u.getAgency() == null || !u.getAgency().getId().equals(agency.getId())) {
            throw new IllegalArgumentException("este código de vendedor não pertence à agência deste formulário.");
        }
        if (u.getAccountKind() != AccountKind.SALES_AGENT && u.getAccountKind() != AccountKind.AGENCY_OWNER) {
            throw new IllegalArgumentException("apenas vendedor ou gestor podem ser indicados no link.");
        }
        return u;
    }

    private PlatformAccount resolveReferralSeller(UUID referralSellerId, Agency agency) {
        if (referralSellerId == null) {
            return null;
        }
        if (agency == null) {
            throw new IllegalArgumentException(
                    "não é possível atribuir vendedor sem configuração de agência para este link.");
        }
        PlatformAccount u = userRepository
                .findById(referralSellerId)
                .orElseThrow(() -> new IllegalArgumentException("usuário de indicação inválido."));
        if (u.getAgency() == null || !u.getAgency().getId().equals(agency.getId())) {
            throw new IllegalArgumentException("o indicador deve pertencer à mesma agência do formulário.");
        }
        if (u.getAccountKind() != AccountKind.SALES_AGENT && u.getAccountKind() != AccountKind.AGENCY_OWNER) {
            throw new IllegalArgumentException("apenas vendedor ou gestor podem ser indicados no link.");
        }
        return u;
    }

    private static boolean hasRouteInfo(JsonNode det) {
        if (det == null || det.isNull()) {
            return false;
        }
        String origem = text(det, "origem");
        if (origem != null && !origem.isBlank()) {
            return true;
        }
        String destinoForm = text(det, "destinoForm");
        if (destinoForm != null && !destinoForm.isBlank()) {
            return true;
        }
        JsonNode arr = det.get("destinosTrechos");
        if (arr != null && arr.isArray()) {
            for (JsonNode n : arr) {
                if (n != null && n.isTextual() && !n.asText().isBlank()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String text(JsonNode det, String field) {
        JsonNode n = det.get(field);
        if (n == null || n.isNull()) {
            return null;
        }
        return n.asText();
    }
}
