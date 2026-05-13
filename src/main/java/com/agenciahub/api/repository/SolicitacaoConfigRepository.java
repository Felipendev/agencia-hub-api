package com.agenciahub.api.repository;

import com.agenciahub.api.entity.SolicitacaoConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoConfigRepository extends JpaRepository<SolicitacaoConfig, UUID> {

    Optional<SolicitacaoConfig> findByAgency_IdAndSlug(UUID agencyId, String slug);

    Optional<SolicitacaoConfig> findFirstByAgency_Id(UUID agencyId);

    @org.springframework.data.jpa.repository.Query("SELECT sc FROM SolicitacaoConfig sc JOIN FETCH sc.agency WHERE sc.slug = :slug")
    Optional<SolicitacaoConfig> findFirstBySlug(@org.springframework.data.repository.query.Param("slug") String slug);
}