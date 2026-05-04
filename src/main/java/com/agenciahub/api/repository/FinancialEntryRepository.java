package com.agenciahub.api.repository;

import com.agenciahub.api.entity.FinancialEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
