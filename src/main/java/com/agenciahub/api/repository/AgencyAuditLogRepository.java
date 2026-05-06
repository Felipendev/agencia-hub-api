package com.agenciahub.api.repository;

import com.agenciahub.api.entity.AgencyAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgencyAuditLogRepository extends JpaRepository<AgencyAuditLog, UUID> {

    List<AgencyAuditLog> findByAgency_IdOrderByCreatedAtDesc(UUID agencyId);
}
