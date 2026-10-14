package com.agenciahub.api.domain.contract;

import com.agenciahub.api.domain.contract.Contracts.PaymentMode;
import com.agenciahub.api.domain.contract.Contracts.Product;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Converte os dados atuais de pessoa, agência e termos no mapa de valores do catálogo. */
public final class ContractValues {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ContractValues() {}

    public record Customer(String name, String email, String phone, JsonNode profile) {}

    public record Agency(String name, String cnpj, String address, String email, String phone) {}

    public record Terms(Product product, String title, String planName, BigDecimal price, LocalDate startDate, LocalDate endDate,
                        PaymentMode paymentMode, Integer installments, String paymentTerms) {}

    public static Map<ContractField, String> resolve(Customer customer, Agency agency, Terms terms, LocalDate issueDate) {
        Map<ContractField, String> values = new EnumMap<>(ContractField.class);
        JsonNode profile = customer.profile();
        put(values, ContractField.CLIENTE_NOME, customer.name());
        put(values, ContractField.CLIENTE_EMAIL, customer.email());
        put(values, ContractField.CLIENTE_TELEFONE, customer.phone());
        put(values, ContractField.CLIENTE_CPF_CNPJ, text(profile, "documentoCpfCnpj"));
        put(values, ContractField.CLIENTE_RG, text(profile, "documentoRg"));
        put(values, ContractField.CLIENTE_NACIONALIDADE, text(profile, "documentoNacionalidade"));
        put(values, ContractField.CLIENTE_ESTADO_CIVIL, text(profile, "documentoEstadoCivil"));
        put(values, ContractField.CLIENTE_PROFISSAO, text(profile, "profissao"));
        put(values, ContractField.CLIENTE_DATA_NASCIMENTO, isoDate(text(profile, "dataNascimento")));
        put(values, ContractField.CLIENTE_ENDERECO, address(profile == null ? null : profile.get("endereco")));
        put(values, ContractField.AGENCIA_NOME, agency.name());
        put(values, ContractField.AGENCIA_CNPJ, agency.cnpj());
        put(values, ContractField.AGENCIA_ENDERECO, agency.address());
        put(values, ContractField.AGENCIA_EMAIL, agency.email());
        put(values, ContractField.AGENCIA_TELEFONE, agency.phone());
        put(values, ContractField.CONTRATO_PRODUTO, terms.product() == null ? null : switch (terms.product()) {
            case TRAVEL -> "Viagens";
            case MILES -> "Milhas";
        });
        put(values, ContractField.CONTRATO_TITULO, terms.title());
        put(values, ContractField.CONTRATO_PLANO, terms.planName());
        put(values, ContractField.CONTRATO_VALOR, terms.price() == null ? null : NumberFormat.getCurrencyInstance(PT_BR).format(terms.price()).replace(' ', ' '));
        put(values, ContractField.CONTRATO_VIGENCIA_INICIO, date(terms.startDate()));
        put(values, ContractField.CONTRATO_VIGENCIA_FIM, date(terms.endDate()));
        put(values, ContractField.CONTRATO_FORMA_PAGAMENTO, paymentMode(terms.paymentMode(), terms.installments()));
        put(values, ContractField.CONTRATO_PARCELAS, terms.paymentMode() == PaymentMode.CASH ? "1"
                : terms.installments() == null ? null : terms.installments().toString());
        put(values, ContractField.CONTRATO_CONDICOES_PAGAMENTO, terms.paymentTerms());
        put(values, ContractField.CONTRATO_DATA_EMISSAO, date(issueDate));
        return values;
    }

    static String paymentMode(PaymentMode mode, Integer installments) {
        if (mode == null) return null;
        return switch (mode) {
            case CASH -> "À vista";
            case INSTALLMENTS -> installments == null ? null : "Parcelado em " + installments + (installments == 1 ? " parcela" : " parcelas");
        };
    }

    static String address(JsonNode node) {
        if (node == null || !node.isObject()) return null;
        String street = join(", ", text(node, "logradouro"), text(node, "numero"), text(node, "complemento"));
        String city = join(" - ", text(node, "cidade"), text(node, "uf"));
        String cep = text(node, "cep");
        return join(", ", street, text(node, "bairro"), city, cep == null ? null : "CEP " + cep, text(node, "pais"));
    }

    private static String isoDate(String raw) {
        if (raw == null) return null;
        try {
            return date(LocalDate.parse(raw.length() >= 10 ? raw.substring(0, 10) : raw));
        } catch (DateTimeParseException e) {
            return raw;
        }
    }

    private static String date(LocalDate date) { return date == null ? null : DATE.format(date); }

    private static String text(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.isContainerNode()) return null;
        String text = value.asText().strip();
        return text.isEmpty() ? null : text;
    }

    private static String join(String separator, String... parts) {
        String joined = Stream.of(parts).filter(Objects::nonNull).filter(p -> !p.isBlank()).collect(Collectors.joining(separator));
        return joined.isEmpty() ? null : joined;
    }

    private static void put(Map<ContractField, String> values, ContractField field, String value) {
        if (value != null && !value.isBlank()) values.put(field, value.strip());
    }
}
