package com.agenciahub.api.application.persistence.repository;

import com.agenciahub.api.application.persistence.entity.ConsentimentoLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ConsentimentoLogRepository extends JpaRepository<ConsentimentoLog, UUID> {

    @Query("SELECT c FROM ConsentimentoLog c WHERE c.submission.agency.id = :agencyId ORDER BY c.createdAt DESC")
    List<ConsentimentoLog> findByAgencyId(@Param("agencyId") UUID agencyId);
}
