package com.agenciahub.api.application.persistence.repository;

import com.agenciahub.api.application.persistence.entity.FinancialEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FinancialEntryRepository extends JpaRepository<FinancialEntry, UUID>,
        JpaSpecificationExecutor<FinancialEntry> {

    @EntityGraph(attributePaths = "customer")
    @Override
    Optional<FinancialEntry> findById(UUID id);

    @EntityGraph(attributePaths = "customer")
    @Override
    java.util.List<FinancialEntry> findAll();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FinancialEntry f SET f.customer = null WHERE f.customer.id = :customerId")
    int unlinkCustomer(@Param("customerId") UUID customerId);

    java.util.List<FinancialEntry> findByAgency_Id(UUID agencyId);
}
