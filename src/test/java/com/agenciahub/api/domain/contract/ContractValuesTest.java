package com.agenciahub.api.domain.contract;

import com.agenciahub.api.domain.contract.Contracts.PaymentMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ContractValuesTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void resolvesCustomerProfileAgencyAndTermsInBrazilianFormat() throws Exception {
        var profile = mapper.readTree("""
                {"documentoCpfCnpj":"123.456.789-00","dataNascimento":"1990-05-20","profissao":" Engenheira ",
                 "endereco":{"logradouro":"Rua A","numero":"10","bairro":"Centro","cidade":"Fortaleza","uf":"CE","cep":"60000-000"}}""");
        var values = ContractValues.resolve(
                new ContractValues.Customer("Ana", "ana@x.com", null, profile),
                new ContractValues.Agency("Agência X", "00.000.000/0001-00", "Av. B, 1", null, null),
                new ContractValues.Terms("Assessoria", "Plano Ouro", new BigDecimal("1234.50"), LocalDate.of(2026, 11, 1),
                        null, PaymentMode.INSTALLMENTS, 3, "Boleto mensal"),
                LocalDate.of(2026, 10, 14));
        assertThat(values)
                .containsEntry(ContractField.CLIENTE_CPF_CNPJ, "123.456.789-00")
                .containsEntry(ContractField.CLIENTE_DATA_NASCIMENTO, "20/05/1990")
                .containsEntry(ContractField.CLIENTE_PROFISSAO, "Engenheira")
                .containsEntry(ContractField.CLIENTE_ENDERECO, "Rua A, 10, Centro, Fortaleza - CE, CEP 60000-000")
                .containsEntry(ContractField.CONTRATO_VALOR, "R$ 1.234,50")
                .containsEntry(ContractField.CONTRATO_VIGENCIA_INICIO, "01/11/2026")
                .containsEntry(ContractField.CONTRATO_FORMA_PAGAMENTO, "Parcelado em 3 parcelas")
                .containsEntry(ContractField.CONTRATO_PARCELAS, "3")
                .containsEntry(ContractField.CONTRATO_DATA_EMISSAO, "14/10/2026")
                .doesNotContainKeys(ContractField.CLIENTE_TELEFONE, ContractField.CONTRATO_VIGENCIA_FIM, ContractField.AGENCIA_EMAIL);
    }

    @Test
    void cashPaymentMeansSingleInstallmentAndMissingProfileIsTolerated() {
        var values = ContractValues.resolve(new ContractValues.Customer("Ana", null, null, null),
                new ContractValues.Agency("X", null, null, null, null),
                new ContractValues.Terms("T", null, null, null, null, PaymentMode.CASH, null, null), LocalDate.of(2026, 1, 1));
        assertThat(values).containsEntry(ContractField.CONTRATO_FORMA_PAGAMENTO, "À vista")
                .containsEntry(ContractField.CONTRATO_PARCELAS, "1")
                .doesNotContainKeys(ContractField.CLIENTE_ENDERECO, ContractField.CONTRATO_VALOR);
    }
}
