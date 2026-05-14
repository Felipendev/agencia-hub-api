package com.agenciahub.api.repository;

import com.agenciahub.api.entity.Quotation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuotationRepository
        extends JpaRepository<Quotation, UUID>, JpaSpecificationExecutor<Quotation> {

    @EntityGraph(attributePaths = {"customer"})
    @Override
    Optional<Quotation> findById(UUID id);

    @EntityGraph(attributePaths = {"customer"})
    List<Quotation> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);
}
