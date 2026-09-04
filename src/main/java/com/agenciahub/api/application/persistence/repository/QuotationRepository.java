package com.agenciahub.api.application.persistence.repository;

import com.agenciahub.api.application.persistence.entity.Quotation;
import com.agenciahub.api.domain.QuotationStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuotationRepository
        extends JpaRepository<Quotation, UUID>, JpaSpecificationExecutor<Quotation> {

    @EntityGraph(attributePaths = {"customer", "seller", "createdByUser"})
    @Override
    Optional<Quotation> findById(UUID id);

    @EntityGraph(attributePaths = {"customer", "seller", "createdByUser"})
    Optional<Quotation> findByIdAndAgency_Id(UUID id, UUID agencyId);

    @EntityGraph(attributePaths = {"customer", "seller", "createdByUser"})
    @Override
    List<Quotation> findAll(Specification<Quotation> spec, Sort sort);

    @EntityGraph(attributePaths = {"customer"})
    List<Quotation> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);

    List<Quotation> findByAgency_Id(UUID agencyId);

    long countByAgency_Id(UUID agencyId);

    @EntityGraph(attributePaths = {"customer", "seller", "agency"})
    @Query("SELECT q FROM Quotation q WHERE q.status IN :statuses AND q.validUntil < :date")
    List<Quotation> findExpired(
            @Param("statuses") List<QuotationStatus> statuses, @Param("date") LocalDate date);

    @EntityGraph(attributePaths = {"customer", "seller", "agency"})
    @Query("SELECT q FROM Quotation q WHERE q.status IN :statuses AND q.validUntil = :date")
    List<Quotation> findByStatusInAndValidUntil(
            @Param("statuses") List<QuotationStatus> statuses, @Param("date") LocalDate date);
}
