package com.agenciahub.api.repository;

import com.agenciahub.api.entity.SolicitacaoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoConfigRepository extends JpaRepository<SolicitacaoConfig, UUID> {

    Optional<SolicitacaoConfig> findByAgency_IdAndSlug(UUID agencyId, String slug);

    @Query("SELECT sc FROM SolicitacaoConfig sc JOIN FETCH sc.agency WHERE sc.agency.id = :agencyId")
    Optional<SolicitacaoConfig> findFirstByAgency_Id(@Param("agencyId") UUID agencyId);

    @Query("SELECT sc FROM SolicitacaoConfig sc JOIN FETCH sc.agency WHERE sc.slug = :slug")
    Optional<SolicitacaoConfig> findFirstBySlug(@Param("slug") String slug);
}