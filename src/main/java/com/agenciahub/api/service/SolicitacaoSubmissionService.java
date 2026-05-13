package com.agenciahub.api.service;

import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.dto.solicitacao.PublicSolicitacaoSubmitRequest;
import com.agenciahub.api.dto.solicitacao.PublicSolicitacaoSubmitResponse;
import com.agenciahub.api.dto.solicitacao.SolicitacaoSubmissionResponse;
import com.agenciahub.api.entity.Agency;
import com.agenciahub.api.entity.SolicitacaoConfig;
import com.agenciahub.api.entity.SolicitacaoSubmission;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.SolicitacaoConfigRepository;
import com.agenciahub.api.repository.SolicitacaoSubmissionRepository;
import com.agenciahub.api.repository.UserRepository;
import com.agenciahub.api.security.TenantContext;
import com.agenciahub.api.validation.PhoneValidator;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SolicitacaoSubmissionService {

    private final SolicitacaoSubmissionRepository submissionRepository;
    private final SolicitacaoConfigRepository configRepository;
    private final UserRepository userRepository;

    @Transactional
    public PublicSolicitacaoSubmitResponse submit(PublicSolicitacaoSubmitRequest request) {
        String telefoneDigits = PhoneValidator.normalize(request.telefone());
        if (!PhoneValidator.isValid(telefoneDigits)) {
            throw new IllegalArgumentException(
                    "Informe um celular válido com DDD (10 ou 11 dígitos).");
        }

        JsonNode detalhes = request.detalhes();
        if (!hasRouteInfo(detalhes)) {
            throw new IllegalArgumentException("Informe origem e/ou destino.");
        }

        var configOpt = configRepository.findFirstBySlug(request.slug().trim());
        Agency agency = configOpt.map(SolicitacaoConfig::getAgency).orElse(null);
        User referral = resolveReferral(request, agency);

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
        return new PublicSolicitacaoSubmitResponse(true, submission.getId());
    }

    private User resolveReferral(PublicSolicitacaoSubmitRequest request, Agency agency) {
        String code = request.sellerPublicCode() != null ? request.sellerPublicCode().trim() : "";
        if (!code.isEmpty()) {
            return resolveReferralByPublicCode(code, agency);
        }
        return resolveReferralSeller(request.referralSellerId(), agency);
    }

    private User resolveReferralByPublicCode(String code, Agency agency) {
        if (agency == null) {
            throw new IllegalArgumentException(
                    "Não é possível atribuir vendedor sem configuração de agência para este link.");
        }
        User u = userRepository.findByPublicLinkCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Código de vendedor inválido ou inexistente."));
        if (u.getAgency() == null || !u.getAgency().getId().equals(agency.getId())) {
            throw new IllegalArgumentException("Este código de vendedor não pertence à agência deste formulário.");
        }
        if (u.getRole() != UserRole.SELLER && u.getRole() != UserRole.OWNER) {
            throw new IllegalArgumentException("Apenas vendedor ou gestor podem ser indicados no link.");
        }
        return u;
    }

    private User resolveReferralSeller(UUID referralSellerId, Agency agency) {
        if (referralSellerId == null) {
            return null;
        }
        if (agency == null) {
            throw new IllegalArgumentException(
                    "Não é possível atribuir vendedor sem configuração de agência para este link.");
        }
        User u = userRepository.findById(referralSellerId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário de indicação inválido."));
        if (u.getAgency() == null || !u.getAgency().getId().equals(agency.getId())) {
            throw new IllegalArgumentException("O indicador deve pertencer à mesma agência do formulário.");
        }
        if (u.getRole() != UserRole.SELLER && u.getRole() != UserRole.OWNER) {
            throw new IllegalArgumentException("Apenas vendedor ou gestor podem ser indicados no link.");
        }
        return u;
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoSubmissionResponse> listForCurrentAgency() {
        UUID agencyId = TenantContext.get();
        if (agencyId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agência não identificada");
        }
        return submissionRepository.findByAgency_IdOrderByCreatedAtDesc(agencyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteForCurrentAgency(UUID id) {
        UUID agencyId = TenantContext.get();
        if (agencyId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agência não identificada");
        }
        var row = submissionRepository.findByIdAndAgency_Id(id, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Submissão não encontrada"));
        submissionRepository.delete(row);
    }

    private SolicitacaoSubmissionResponse toResponse(SolicitacaoSubmission s) {
        User ref = s.getReferralSeller();
        UUID refId = ref != null ? ref.getId() : null;
        String refName = ref != null ? ref.getName() : null;
        return new SolicitacaoSubmissionResponse(
                s.getId(),
                s.getSlug(),
                s.getCreatedAt(),
                s.getNome(),
                s.getEmail(),
                s.getTelefone(),
                refId,
                refName,
                s.getDetalhes(),
                s.getObservacoes());
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
