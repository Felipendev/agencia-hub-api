package com.agenciahub.api.repository;

import com.agenciahub.api.entity.Opportunity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {

    @EntityGraph(attributePaths = "customer")
    List<Opportunity> findByCustomer_IdOrderByExpectedTravelDateDesc(UUID customerId);

    @EntityGraph(attributePaths = "customer")
    List<Opportunity> findAllByOrderByExpectedTravelDateDesc();

    @EntityGraph(attributePaths = "customer")
    @Override
    Optional<Opportunity> findById(UUID id);
}
