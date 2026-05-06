package com.agenciahub.api.repository;

import com.agenciahub.api.entity.TermsAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TermsAcceptanceRepository extends JpaRepository<TermsAcceptance, UUID> {

    Optional<TermsAcceptance> findFirstByUser_IdOrderByAcceptedAtDesc(UUID userId);

    List<TermsAcceptance> findByUser_IdOrderByAcceptedAtDesc(UUID userId);
}
