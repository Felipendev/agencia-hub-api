package com.agenciahub.api.application.persistence.repository;

import com.agenciahub.api.application.persistence.entity.ConsentimentoLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsentimentoLogRepository extends JpaRepository<ConsentimentoLog, UUID> {
}
