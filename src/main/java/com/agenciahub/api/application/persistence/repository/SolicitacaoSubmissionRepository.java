package com.agenciahub.api.application.persistence.repository;

import com.agenciahub.api.application.persistence.entity.SolicitacaoSubmission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoSubmissionRepository extends JpaRepository<SolicitacaoSubmission, UUID> {

    /** Includes imported rows for administrative exports and account deletion. */
    @EntityGraph(attributePaths = {"referralSeller"})
    List<SolicitacaoSubmission> findByAgency_IdOrderByCreatedAtDesc(UUID agencyId);

    @EntityGraph(attributePaths = {"referralSeller"})
    @Query("""
            select s from SolicitacaoSubmission s
            where s.agency.id = :agencyId
              and not exists (
                select q.id from Quotation q where q.publicSubmission = s
              )
            order by s.createdAt desc
            """)
    List<SolicitacaoSubmission> findUnimportedByAgencyIdOrderByCreatedAtDesc(
            @Param("agencyId") UUID agencyId);

    @EntityGraph(attributePaths = {"referralSeller"})
    @Query("""
            select s from SolicitacaoSubmission s
            where s.agency.id = :agencyId
              and s.referralSeller.id = :referralSellerId
              and not exists (
                select q.id from Quotation q where q.publicSubmission = s
              )
            order by s.createdAt desc
            """)
    List<SolicitacaoSubmission> findUnimportedByAgencyIdAndReferralSellerIdOrderByCreatedAtDesc(
            @Param("agencyId") UUID agencyId,
            @Param("referralSellerId") UUID referralSellerId);

    List<SolicitacaoSubmission> findByReferralSeller_IdAndCreatedAtAfterOrderByCreatedAtDesc(UUID referralSellerId, Instant after);

    Optional<SolicitacaoSubmission> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<SolicitacaoSubmission> findByEmailAndTelefone(String email, String telefone);

    List<SolicitacaoSubmission> findByEmailIgnoreCase(String email);

    List<SolicitacaoSubmission> findByEmailIgnoreCaseAndAgency_Id(String email, UUID agencyId);
}
