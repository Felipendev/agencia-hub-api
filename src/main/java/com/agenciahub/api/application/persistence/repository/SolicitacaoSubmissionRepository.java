package com.agenciahub.api.application.persistence.repository;

import com.agenciahub.api.application.persistence.entity.SolicitacaoSubmission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoSubmissionRepository extends JpaRepository<SolicitacaoSubmission, UUID> {

    @EntityGraph(attributePaths = {"referralSeller"})
    List<SolicitacaoSubmission> findByAgency_IdOrderByCreatedAtDesc(UUID agencyId);

    @EntityGraph(attributePaths = {"referralSeller"})
    List<SolicitacaoSubmission> findByAgency_IdAndReferralSeller_IdOrderByCreatedAtDesc(UUID agencyId, UUID referralSellerId);

    Optional<SolicitacaoSubmission> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<SolicitacaoSubmission> findByEmailAndTelefone(String email, String telefone);
}
