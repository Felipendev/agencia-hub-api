package com.agenciahub.api.domain.contract;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Catálogo fechado de campos que um modelo de contrato pode usar como {@code {{chave}}}.
 * O texto jurídico é sempre fornecido pela agência; aqui só existem os dados preenchidos.
 */
public enum ContractField {
    CLIENTE_NOME("cliente.nome", "Nome da pessoa", Source.CUSTOMER),
    CLIENTE_CPF_CNPJ("cliente.cpfCnpj", "CPF/CNPJ da pessoa", Source.CUSTOMER),
    CLIENTE_RG("cliente.rg", "RG da pessoa", Source.CUSTOMER),
    CLIENTE_EMAIL("cliente.email", "E-mail da pessoa", Source.CUSTOMER),
    CLIENTE_TELEFONE("cliente.telefone", "Telefone da pessoa", Source.CUSTOMER),
    CLIENTE_ENDERECO("cliente.endereco", "Endereço da pessoa", Source.CUSTOMER),
    CLIENTE_NACIONALIDADE("cliente.nacionalidade", "Nacionalidade da pessoa", Source.CUSTOMER),
    CLIENTE_ESTADO_CIVIL("cliente.estadoCivil", "Estado civil da pessoa", Source.CUSTOMER),
    CLIENTE_PROFISSAO("cliente.profissao", "Profissão da pessoa", Source.CUSTOMER),
    CLIENTE_DATA_NASCIMENTO("cliente.dataNascimento", "Data de nascimento da pessoa", Source.CUSTOMER),
    AGENCIA_NOME("agencia.nome", "Nome da agência", Source.AGENCY),
    AGENCIA_CNPJ("agencia.cnpj", "CNPJ da agência", Source.AGENCY),
    AGENCIA_ENDERECO("agencia.endereco", "Endereço da agência", Source.AGENCY),
    AGENCIA_EMAIL("agencia.email", "E-mail comercial da agência", Source.AGENCY),
    AGENCIA_TELEFONE("agencia.telefone", "Telefone da agência", Source.AGENCY),
    CONTRATO_PRODUTO("contrato.produto", "Produto contratado (Viagens ou Milhas)", Source.CONTRACT),
    CONTRATO_TITULO("contrato.titulo", "Título do contrato", Source.CONTRACT),
    CONTRATO_PLANO("contrato.plano", "Plano ou serviço contratado", Source.CONTRACT),
    CONTRATO_VALOR("contrato.valor", "Valor total", Source.CONTRACT),
    CONTRATO_VIGENCIA_INICIO("contrato.vigenciaInicio", "Início da vigência", Source.CONTRACT),
    CONTRATO_VIGENCIA_FIM("contrato.vigenciaFim", "Fim da vigência", Source.CONTRACT),
    CONTRATO_FORMA_PAGAMENTO("contrato.formaPagamento", "Forma de pagamento", Source.CONTRACT),
    CONTRATO_PARCELAS("contrato.parcelas", "Número de parcelas", Source.CONTRACT),
    CONTRATO_CONDICOES_PAGAMENTO("contrato.condicoesPagamento", "Condições de pagamento", Source.CONTRACT),
    CONTRATO_DATA_EMISSAO("contrato.dataEmissao", "Data de emissão", Source.CONTRACT);

    public enum Source { CUSTOMER, AGENCY, CONTRACT }

    private static final Map<String, ContractField> BY_KEY = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ContractField::key, Function.identity()));

    private final String key;
    private final String label;
    private final Source source;

    ContractField(String key, String label, Source source) {
        this.key = key;
        this.label = label;
        this.source = source;
    }

    public String key() { return key; }
    public String label() { return label; }
    public Source source() { return source; }

    public static Optional<ContractField> byKey(String key) {
        return Optional.ofNullable(BY_KEY.get(key));
    }
}
