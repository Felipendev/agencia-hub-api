package com.agenciahub.api.application.persistence.repository;

import com.agenciahub.api.application.persistence.entity.Quotation;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuotationRepository
        extends JpaRepository<Quotation, UUID>, JpaSpecificationExecutor<Quotation> {

    @EntityGraph(attributePaths = {"customer", "seller", "createdByUser"})
    @Override
    Optional<Quotation> findById(UUID id);

    @EntityGraph(attributePaths = {"customer", "seller", "createdByUser"})
    @Override
    List<Quotation> findAll(Specification<Quotation> spec, Sort sort);

    @EntityGraph(attributePaths = {"customer"})
    List<Quotation> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);

    List<Quotation> findByAgency_Id(UUID agencyId);

    long countByAgency_Id(UUID agencyId);
}
