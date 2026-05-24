package com.agenciahub.api.application.usecases.solicitacao;

import com.agenciahub.api.application.usecases.solicitacao.pub.submit.SolicitacaoDetalhesValidator;
import com.agenciahub.api.exception.DetalhesValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SolicitacaoDetalhesValidatorTest {

    private SolicitacaoDetalhesValidator validator;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        validator = new SolicitacaoDetalhesValidator();
    }

    // ── Tamanho ────────────────────────────────────────────────────────────────

    @Test
    void validate_whenPayloadExceeds64KB_throwsDetalhesValidationException() throws Exception {
        ObjectNode node = mapper.createObjectNode();
        node.put("origem", "x".repeat(70_000));
        assertThatThrownBy(() -> validator.validate(node))
                .isInstanceOf(DetalhesValidationException.class)
                .hasMessageContaining("excede limite");
    }

    // ── Campos desconhecidos ───────────────────────────────────────────────────

    @Test
    void validate_whenUnknownFieldPresent_throwsDetalhesValidationException() throws Exception {
        ObjectNode node = mapper.createObjectNode();
        node.put("origem", "São Paulo");
        node.put("campoDesconhecido", "valor");
        assertThatThrownBy(() -> validator.validate(node))
                .isInstanceOf(DetalhesValidationException.class)
                .hasMessageContaining("campoDesconhecido");
    }

    // ── Datas ─────────────────────────────────────────────────────────────────

    @Test
    void validate_whenDataIdaHasInvalidFormat_throwsDetalhesValidationException() throws Exception {
        ObjectNode node = mapper.createObjectNode();
        node.put("dataIda", "31/12/2025");
        assertThatThrownBy(() -> validator.validate(node))
                .isInstanceOf(DetalhesValidationException.class)
                .hasMessageContaining("dataIda")
                .hasMessageContaining("YYYY-MM-DD");
    }

    @Test
    void validate_whenDataIdaIsValidIsoDate_doesNotThrow() throws Exception {
        ObjectNode node = mapper.createObjectNode();
        node.put("dataIda", "2025-12-31");
        assertThat(validator.validate(node)).isNotNull();
    }

    @Test
    void validate_whenDataVoltaHasInvalidFormat_throwsDetalhesValidationException() throws Exception {
        ObjectNode node = mapper.createObjectNode();
        node.put("dataVolta", "not-a-date");
        assertThatThrownBy(() -> validator.validate(node))
                .isInstanceOf(DetalhesValidationException.class)
                .hasMessageContaining("dataVolta");
    }

    // ── Contagem de passageiros ────────────────────────────────────────────────

    @Test
    void validate_whenAdultosIsNegative_throwsDetalhesValidationException() throws Exception {
        ObjectNode node = mapper.createObjectNode();
        node.put("adultos", -1);
        assertThatThrownBy(() -> validator.validate(node))
                .isInstanceOf(DetalhesValidationException.class)
                .hasMessageContaining("adultos");
    }

    @Test
    void validate_whenAdultosExceedsMax_throwsDetalhesValidationException() throws Exception {
        ObjectNode node = mapper.createObjectNode();
        node.put("adultos", 51);
        assertThatThrownBy(() -> validator.validate(node))
                .isInstanceOf(DetalhesValidationException.class)
                .hasMessageContaining("adultos");
    }

    @Test
    void validate_whenCriancasExceedsMax_throwsDetalhesValidationException() throws Exception {
        ObjectNode node = mapper.createObjectNode();
        node.put("criancas", 100);
        assertThatThrownBy(() -> validator.validate(node))
                .isInstanceOf(DetalhesValidationException.class)
                .hasMessageContaining("criancas");
    }

    @Test
    void validate_whenCountsAreValid_doesNotThrow() throws Exception {
        ObjectNode node = mapper.createObjectNode();
        node.put("adultos", 2);
        node.put("criancas", 1);
        node.put("bebes", 0);
        assertThat(validator.validate(node)).isNotNull();
    }

    // ── Sanitização HTML ───────────────────────────────────────────────────────

    @Test
    void validate_whenOrigimContainsHtmlTags_returnsSanitizedNode() throws Exception {
        ObjectNode node = mapper.createObjectNode();
        node.put("origem", "<script>alert('xss')</script>São Paulo");
        JsonNode result = validator.validate(node);
        assertThat(result.get("origem").asText()).doesNotContain("<script>");
        assertThat(result.get("origem").asText()).contains("São Paulo");
    }

    // ── Null e vazio ───────────────────────────────────────────────────────────

    @Test
    void validate_whenDetalhesIsNull_returnsNull() {
        assertThat(validator.validate(null)).isNull();
    }

    @Test
    void validate_whenDetalhesIsEmptyObject_doesNotThrow() throws Exception {
        ObjectNode node = mapper.createObjectNode();
        assertThat(validator.validate(node)).isNotNull();
    }
}
