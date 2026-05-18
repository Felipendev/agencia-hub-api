package com.agenciahub.api.application.persistence.repository;

import com.agenciahub.api.application.persistence.entity.SolicitacaoConfig;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoConfigRepository extends JpaRepository<SolicitacaoConfig, UUID> {

    Optional<SolicitacaoConfig> findByAgency_IdAndSlug(UUID agencyId, String slug);

    /** Retorna a primeira config da agência (LIMIT 1). @EntityGraph carrega agency imediatamente. */
    @EntityGraph(attributePaths = {"agency"})
    Optional<SolicitacaoConfig> findFirstByAgency_Id(UUID agencyId);

    /** Busca config pelo slug público; @EntityGraph carrega agency imediatamente. */
    @EntityGraph(attributePaths = {"agency"})
    Optional<SolicitacaoConfig> findFirstBySlug(String slug);
}