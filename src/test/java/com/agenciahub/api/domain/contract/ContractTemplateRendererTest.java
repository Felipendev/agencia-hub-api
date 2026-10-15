package com.agenciahub.api.domain.contract;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractTemplateRendererTest {

    @Test
    void validateReturnsUsedFieldsInOrderWithoutDuplicates() {
        var used = ContractTemplateRenderer.validate("{{cliente.nome}} contrata {{ contrato.plano }} — {{cliente.nome}}");
        assertThat(used).containsExactly(ContractField.CLIENTE_NOME, ContractField.CONTRATO_PLANO);
    }

    @Test
    void validateRejectsUnknownPlaceholdersAndBlankOrHugeBodies() {
        assertThatThrownBy(() -> ContractTemplateRenderer.validate("Olá {{cliente.apelido}} e {{senha}}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("{{cliente.apelido}}").hasMessageContaining("{{senha}}");
        assertThatThrownBy(() -> ContractTemplateRenderer.validate("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ContractTemplateRenderer.validate("x".repeat(ContractTemplateRenderer.MAX_BODY_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void renderReplacesValuesAndHighlightsMissingFields() {
        var rendered = ContractTemplateRenderer.render(
                "Contratante: {{cliente.nome}}, CPF {{cliente.cpfCnpj}}. Valor {{contrato.valor}}.",
                Map.of(ContractField.CLIENTE_NOME, "Ana $1 \\ Souza", ContractField.CONTRATO_VALOR, "R$ 10,00"));
        assertThat(rendered.content()).isEqualTo("Contratante: Ana $1 \\ Souza, CPF [CPF/CNPJ DA PESSOA PENDENTE]. Valor R$ 10,00.");
        assertThat(rendered.missing()).isEqualTo(List.of(ContractField.CLIENTE_CPF_CNPJ));
    }

    @Test
    void textWithoutPlaceholdersIsKeptVerbatim() {
        String body = "Cláusula 1ª — texto fornecido pela agência { não é campo }.";
        assertThat(ContractTemplateRenderer.render(body, Map.of()).content()).isEqualTo(body);
        assertThat(ContractTemplateRenderer.validate(body)).isEmpty();
    }
}
