package com.agenciahub.api.application.usecases.solicitacao.pub.submit;

import com.agenciahub.api.exception.DetalhesValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Component
public class SolicitacaoDetalhesValidator {

    private static final int MAX_JSON_BYTES = 65_536; // 64 KB
    private static final int MAX_PASSENGERS = 50;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "servicosDesejados", "origem", "destinoForm", "destinosTrechos",
            "dataIda", "dataVolta", "flexibilidadeIda", "flexibilidadeVolta",
            "flexibilidadeIdaOutro", "flexibilidadeVoltaOutro", "horarioSaidaIda",
            "horarioSaidaVolta", "preferenciaVooIda", "preferenciaVooVolta",
            "adultos", "criancas", "idadesCriancas", "bebes",
            "malasDespachadas", "qtdMalas", "bagagemEspecial",
            "categoriaHospedagem", "comodidadesHospedagem", "qtdQuartos",
            "celular", "whatsapp", "whatsappIgualCelular",
            "preferenciaComunicacao", "usaMilhas",
            "formaPagamento", "formaPagamentoOutro",
            "cupomCodigo", "cupomValidoAte"
    );

    private static final Set<String> DATE_FIELDS = Set.of("dataIda", "dataVolta", "cupomValidoAte");
    private static final Set<String> PASSENGER_COUNT_FIELDS = Set.of("adultos", "criancas", "bebes");

    public JsonNode validate(JsonNode detalhes) {
        if (detalhes == null || detalhes.isNull()) {
            return detalhes;
        }

        try {
            byte[] bytes = MAPPER.writeValueAsBytes(detalhes);
            if (bytes.length > MAX_JSON_BYTES) {
                throw new DetalhesValidationException("payload de detalhes excede limite de 64KB");
            }
        } catch (JsonProcessingException e) {
            throw new DetalhesValidationException("detalhes com formato JSON inválido");
        }

        List<String> violations = new ArrayList<>();

        Iterator<String> fieldNames = detalhes.fieldNames();
        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            if (!ALLOWED_FIELDS.contains(field)) {
                violations.add("campo desconhecido: '" + field + "'");
            }
        }

        for (String dateField : DATE_FIELDS) {
            JsonNode val = detalhes.get(dateField);
            if (val != null && !val.isNull() && val.isTextual() && !val.asText().isBlank()) {
                try {
                    LocalDate.parse(val.asText());
                } catch (DateTimeParseException e) {
                    violations.add("campo '" + dateField + "': formato inválido, use YYYY-MM-DD");
                }
            }
        }

        for (String countField : PASSENGER_COUNT_FIELDS) {
            JsonNode val = detalhes.get(countField);
            if (val != null && !val.isNull()) {
                if (!val.isIntegralNumber()) {
                    violations.add("campo '" + countField + "': deve ser um número inteiro");
                } else {
                    int count = val.asInt();
                    if (count < 0 || count > MAX_PASSENGERS) {
                        violations.add("campo '" + countField + "': deve ser entre 0 e " + MAX_PASSENGERS);
                    }
                }
            }
        }

        if (!violations.isEmpty()) {
            throw new DetalhesValidationException(String.join("; ", violations));
        }

        return sanitize(detalhes);
    }

    private JsonNode sanitize(JsonNode node) {
        ObjectNode result = MAPPER.createObjectNode();
        node.fields().forEachRemaining(entry -> {
            JsonNode val = entry.getValue();
            if (val.isTextual()) {
                result.put(entry.getKey(), stripHtml(val.asText()));
            } else {
                result.set(entry.getKey(), val);
            }
        });
        return result;
    }

    private static String stripHtml(String text) {
        return text == null ? null : text.replaceAll("<[^>]*>", "");
    }
}
