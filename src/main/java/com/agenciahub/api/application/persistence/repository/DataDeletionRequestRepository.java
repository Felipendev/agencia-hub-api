package com.agenciahub.api.application.persistence.repository;

import com.agenciahub.api.application.persistence.entity.DataDeletionRequest;
import com.agenciahub.api.domain.DataDeletionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataDeletionRequestRepository extends JpaRepository<DataDeletionRequest, UUID> {

    Optional<DataDeletionRequest> findFirstByEmailIgnoreCaseAndStatusAndCreatedAtAfter(
            String email, DataDeletionStatus status, Instant since);

    List<DataDeletionRequest> findByStatus(DataDeletionStatus status);
}
