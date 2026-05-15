package com.agenciahub.api.application.usecases.solicitacao;

import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigResponse;
import com.agenciahub.api.entity.Agency;
import com.agenciahub.api.entity.SolicitacaoConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/** Mapeamentos e defaults partilhados entre use cases de configuração de solicitação. */
public final class SolicitacaoConfigSupport {

    private SolicitacaoConfigSupport() {}

    public static ArrayNode emptyArray(ObjectMapper objectMapper) {
        return objectMapper.createArrayNode();
    }

    public static JsonNode defaultLinks(ObjectMapper objectMapper) {
        try {
            String json =
                    """
                    [
                      {"id":"d1","tipo":"whatsapp","url":"https://wa.me/","label":"WhatsApp"},
                      {"id":"d2","tipo":"instagram","url":"https://instagram.com/","label":"Instagram"},
                      {"id":"d3","tipo":"email","url":"mailto:contato@agencia.com","label":"E-mail"}
                    ]
                    """;
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return emptyArray(objectMapper);
        }
    }

    public static SolicitacaoConfigResponse toResponse(SolicitacaoConfig config) {
        return new SolicitacaoConfigResponse(
                config.getSlug(),
                config.getTituloPagina(),
                config.getTextoIntro(),
                config.getLogoDataUrl(),
                config.getNomeMarca(),
                config.getLinksSociais());
    }

    public static SolicitacaoConfigResponse withAgencyLogoFallback(SolicitacaoConfigResponse response, Agency agency) {
        if (response.logoDataUrl() == null || response.logoDataUrl().isBlank()) {
            if (agency != null && agency.getLogoUrl() != null && !agency.getLogoUrl().isBlank()) {
                return new SolicitacaoConfigResponse(
                        response.slug(),
                        response.tituloPagina(),
                        response.textoIntro(),
                        agency.getLogoUrl(),
                        response.nomeMarca(),
                        response.linksSociais());
            }
        }
        return response;
    }

    public static SolicitacaoConfigResponse defaultResponse(String slug, ObjectMapper objectMapper) {
        return new SolicitacaoConfigResponse(
                slug,
                "Solicitação de Orçamento",
                "Preencha os dados abaixo em poucos minutos. Nossa equipe retorna o mais rápido possível, priorizando viagens com datas mais próximas.",
                null,
                "AgenciaHub",
                defaultLinks(objectMapper));
    }

    public static SolicitacaoConfig newDefaultForAgency(Agency agency, ObjectMapper objectMapper) {
        String defaultSlug = agency.getName() != null
                ? agency.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "")
                : "minha-agencia";
        String slug = defaultSlug;
        if (slug.length() < 2) slug = "minha-agencia";
        if (slug.length() > 64) slug = slug.substring(0, 64);

        return SolicitacaoConfig.builder()
                .agency(agency)
                .slug(slug)
                .tituloPagina("Solicitação de Orçamento")
                .textoIntro(
                        "Preencha os dados abaixo em poucos minutos. Nossa equipe retorna o mais rápido possível, priorizando viagens com datas mais próximas.")
                .logoDataUrl(null)
                .nomeMarca(agency.getName() != null ? agency.getName() : "AgênciasHub")
                .linksSociais(defaultLinks(objectMapper))
                .build();
    }
}
