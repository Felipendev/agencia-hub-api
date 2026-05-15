package com.agenciahub.api.application.usecases.solicitacao.pub.submit;

import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.application.usecases.solicitacao.pub.submit.PublicSolicitacaoSubmitRequestDTO;
import com.agenciahub.api.application.usecases.solicitacao.pub.submit.PublicSolicitacaoSubmitResponseDTO;
import com.agenciahub.api.entity.Agency;
import com.agenciahub.api.entity.SolicitacaoConfig;
import com.agenciahub.api.entity.SolicitacaoSubmission;
import com.agenciahub.api.entity.PlatformAccount;
import com.agenciahub.api.repository.SolicitacaoConfigRepository;
import com.agenciahub.api.repository.SolicitacaoSubmissionRepository;
import com.agenciahub.api.repository.PlatformAccountRepository;
import com.agenciahub.api.validation.PhoneValidator;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubmitPublicSolicitacao implements SubmitPublicSolicitacaoUseCase {

    private final SolicitacaoSubmissionRepository submissionRepository;
    private final SolicitacaoConfigRepository configRepository;
    private final PlatformAccountRepository userRepository;

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
                .build();

        submission = submissionRepository.save(submission);
        return new PublicSolicitacaoSubmitResponseDTO(true, submission.getId());
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
        if (u.getRole() != AccountKind.SALES_AGENT && u.getRole() != AccountKind.AGENCY_OWNER) {
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
        if (u.getRole() != AccountKind.SALES_AGENT && u.getRole() != AccountKind.AGENCY_OWNER) {
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
