package com.agenciahub.api.application.persistence.repository;

import com.agenciahub.api.domain.InvitationStatus;
import com.agenciahub.api.application.persistence.entity.Invitation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByToken(String token);

    @EntityGraph(attributePaths = "agency")
    Optional<Invitation> findWithAgencyByToken(String token);

    List<Invitation> findByAgency_IdOrderByCreatedAtDesc(UUID agencyId);

    List<Invitation> findByAgency_IdAndStatus(UUID agencyId, InvitationStatus status);

    Optional<Invitation> findByEmailAndStatus(String email, InvitationStatus status);
}
