package com.agenciahub.api.repository;

import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.entity.Quotation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuotationRepository
        extends JpaRepository<Quotation, UUID>, JpaSpecificationExecutor<Quotation> {

    @EntityGraph(attributePaths = {"customer", "opportunity"})
    @Override
    Optional<Quotation> findById(UUID id);

    @EntityGraph(attributePaths = {"customer", "opportunity"})
    List<Quotation> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"customer", "opportunity"})
    List<Quotation> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);

    @EntityGraph(attributePaths = {"customer", "opportunity"})
    List<Quotation> findByStatusOrderByCreatedAtDesc(QuotationStatus status);

    @EntityGraph(attributePaths = {"customer", "opportunity"})
    List<Quotation> findByCustomer_IdAndStatusOrderByCreatedAtDesc(UUID customerId, QuotationStatus status);

    // ── Soft-delete queries ───────────────────────────────────────────────────

    @EntityGraph(attributePaths = {"customer", "opportunity"})
    @Query("SELECT q FROM Quotation q WHERE q.deletedAt IS NULL ORDER BY q.createdAt DESC")
    List<Quotation> findAllActive();

    @EntityGraph(attributePaths = {"customer", "opportunity"})
    @Query("SELECT q FROM Quotation q WHERE q.deletedAt IS NOT NULL ORDER BY q.deletedAt DESC")
    List<Quotation> findAllDeleted();

    @EntityGraph(attributePaths = {"customer", "opportunity"})
    @Query("SELECT q FROM Quotation q WHERE q.id = :id AND q.deletedAt IS NULL")
    Optional<Quotation> findActiveById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"customer", "opportunity"})
    @Query("SELECT q FROM Quotation q WHERE q.id = :id AND q.deletedAt IS NOT NULL")
    Optional<Quotation> findDeletedById(@Param("id") UUID id);

    // ── Multi-tenancy (agency_id filtering) ──────────────────────────────────

    @EntityGraph(attributePaths = {"customer", "opportunity"})
    @Query("SELECT q FROM Quotation q WHERE q.agency.id = :agencyId AND q.deletedAt IS NULL ORDER BY q.createdAt DESC")
    List<Quotation> findAllActiveByAgencyId(@Param("agencyId") UUID agencyId);

    @EntityGraph(attributePaths = {"customer", "opportunity"})
    @Query("SELECT q FROM Quotation q WHERE q.agency.id = :agencyId AND q.deletedAt IS NOT NULL ORDER BY q.deletedAt DESC")
    List<Quotation> findAllDeletedByAgencyId(@Param("agencyId") UUID agencyId);
}
