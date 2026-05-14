package com.agenciahub.api.service;

import com.agenciahub.api.application.solicitacao.SolicitacaoSubmissionResponseMapper;
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
    private final SolicitacaoSubmissionResponseMapper solicitacaoSubmissionResponseMapper;

    @Transactional
    public PublicSolicitacaoSubmitResponse submit(PublicSolicitacaoSubmitRequest request) {
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
                    "não é possível atribuir vendedor sem configuração de agência para este link.");
        }
        User u = userRepository.findByPublicLinkCode(code)
                .orElseThrow(() -> new IllegalArgumentException("código de vendedor inválido ou inexistente."));
        if (u.getAgency() == null || !u.getAgency().getId().equals(agency.getId())) {
            throw new IllegalArgumentException("este código de vendedor não pertence à agência deste formulário.");
        }
        if (u.getRole() != UserRole.SELLER && u.getRole() != UserRole.OWNER) {
            throw new IllegalArgumentException("apenas vendedor ou gestor podem ser indicados no link.");
        }
        return u;
    }

    private User resolveReferralSeller(UUID referralSellerId, Agency agency) {
        if (referralSellerId == null) {
            return null;
        }
        if (agency == null) {
            throw new IllegalArgumentException(
                    "não é possível atribuir vendedor sem configuração de agência para este link.");
        }
        User u = userRepository.findById(referralSellerId)
                .orElseThrow(() -> new IllegalArgumentException("usuário de indicação inválido."));
        if (u.getAgency() == null || !u.getAgency().getId().equals(agency.getId())) {
            throw new IllegalArgumentException("o indicador deve pertencer à mesma agência do formulário.");
        }
        if (u.getRole() != UserRole.SELLER && u.getRole() != UserRole.OWNER) {
            throw new IllegalArgumentException("apenas vendedor ou gestor podem ser indicados no link.");
        }
        return u;
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoSubmissionResponse> listForAgency(UUID agencyId) {
        if (agencyId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "agência não identificada");
        }
        return submissionRepository.findByAgency_IdOrderByCreatedAtDesc(agencyId).stream()
                .map(solicitacaoSubmissionResponseMapper::toResponse)
                .toList();
    }

    @Transactional
    public void deleteForAgency(UUID id, UUID agencyId) {
        if (agencyId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "agência não identificada");
        }
        var row = submissionRepository.findByIdAndAgency_Id(id, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("submissão não encontrada"));
        submissionRepository.delete(row);
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
