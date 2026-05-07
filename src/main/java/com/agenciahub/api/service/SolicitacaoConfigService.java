package com.agenciahub.api.service;

import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigRequest;
import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigResponse;
import com.agenciahub.api.entity.Agency;
import com.agenciahub.api.entity.SolicitacaoConfig;
import com.agenciahub.api.repository.SolicitacaoConfigRepository;
import com.agenciahub.api.security.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SolicitacaoConfigService {

    private final SolicitacaoConfigRepository repository;
    private final AgencyService agencyService;
    private final ObjectMapper objectMapper;

    /**
     * Returns config for the current agency + slug, creating a default if not found.
     */
    @Transactional
    public SolicitacaoConfigResponse getOrCreateDefault(String slug) {
        UUID agencyId = TenantContext.get();
        if (agencyId == null) {
            throw new IllegalStateException("Nenhuma agência no contexto do tenant");
        }

        SolicitacaoConfig config = repository.findByAgency_IdAndSlug(agencyId, slug)
                .orElseGet(() -> createDefault(agencyId, slug));

        return toResponse(config);
    }

    /**
     * Upserts config for the current agency.
     */
    @Transactional
    public SolicitacaoConfigResponse upsert(SolicitacaoConfigRequest request) {
        UUID agencyId = TenantContext.get();
        if (agencyId == null) {
            throw new IllegalStateException("Nenhuma agência no contexto do tenant");
        }

        SolicitacaoConfig config = repository.findByAgency_IdAndSlug(agencyId, request.slug())
                .orElseGet(() -> {
                    Agency agency = agencyService.getById(agencyId);
                    return SolicitacaoConfig.builder()
                            .agency(agency)
                            .slug(request.slug())
                            .build();
                });

        config.setTituloPagina(request.tituloPagina());
        config.setTextoIntro(request.textoIntro() != null ? request.textoIntro() : "");
        config.setLogoDataUrl(request.logoDataUrl());
        config.setNomeMarca(request.nomeMarca() != null && !request.nomeMarca().isBlank()
                ? request.nomeMarca() : "Agência");
        config.setLinksSociais(request.linksSociais() != null
                ? request.linksSociais() : emptyArray());

        config = repository.save(config);
        return toResponse(config);
    }

    /**
     * Returns config by slug only (no tenant context needed) — for public access.
     */
    @Transactional(readOnly = true)
    public SolicitacaoConfigResponse getPublicBySlug(String slug) {
        return repository.findFirstBySlug(slug)
                .map(this::toResponse)
                .orElse(defaultResponse(slug));
    }

    private SolicitacaoConfig createDefault(UUID agencyId, String slug) {
        Agency agency = agencyService.getById(agencyId);
        SolicitacaoConfig config = SolicitacaoConfig.builder()
                .agency(agency)
                .slug(slug)
                .tituloPagina("Solicitação de Orçamento")
                .textoIntro("Preencha os dados abaixo em poucos minutos. Nossa equipe retorna o mais rápido possível, priorizando viagens com datas mais próximas.")
                .logoDataUrl(null)
                .nomeMarca("AgenciaHub")
                .linksSociais(defaultLinks())
                .build();
        return repository.save(config);
    }

    private SolicitacaoConfigResponse defaultResponse(String slug) {
        return new SolicitacaoConfigResponse(
                slug,
                "Solicitação de Orçamento",
                "Preencha os dados abaixo em poucos minutos. Nossa equipe retorna o mais rápido possível, priorizando viagens com datas mais próximas.",
                null,
                "AgenciaHub",
                defaultLinks()
        );
    }

    private JsonNode defaultLinks() {
        try {
            String json = """
                [
                  {"id":"d1","tipo":"whatsapp","url":"https://wa.me/","label":"WhatsApp"},
                  {"id":"d2","tipo":"instagram","url":"https://instagram.com/","label":"Instagram"},
                  {"id":"d3","tipo":"email","url":"mailto:contato@agencia.com","label":"E-mail"}
                ]
                """;
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return emptyArray();
        }
    }

    private ArrayNode emptyArray() {
        return objectMapper.createArrayNode();
    }

    private SolicitacaoConfigResponse toResponse(SolicitacaoConfig config) {
        return new SolicitacaoConfigResponse(
                config.getSlug(),
                config.getTituloPagina(),
                config.getTextoIntro(),
                config.getLogoDataUrl(),
                config.getNomeMarca(),
                config.getLinksSociais()
        );
    }
}
