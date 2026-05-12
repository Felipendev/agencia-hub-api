package com.agenciahub.api.repository;

import com.agenciahub.api.entity.SolicitacaoConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoConfigRepository extends JpaRepository<SolicitacaoConfig, UUID> {

    Optional<SolicitacaoConfig> findByAgency_IdAndSlug(UUID agencyId, String slug);

    List<SolicitacaoConfig> findByAgency_Id(UUID agencyId);

    Optional<SolicitacaoConfig> findFirstBySlug(String slug);

    /** Última configuração alterada da agência (para o painel sem slug fixo na URL). */
    Optional<SolicitacaoConfig> findFirstByAgency_IdOrderByUpdatedAtDesc(UUID agencyId);
}
