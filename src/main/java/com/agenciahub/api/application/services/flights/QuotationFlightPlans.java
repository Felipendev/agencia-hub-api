package com.agenciahub.api.application.services.flights;

import com.agenciahub.api.application.persistence.entity.Quotation;
import com.agenciahub.api.security.SecurityContextUsers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class QuotationFlightPlans {
    private final JdbcTemplate jdbc;
    public QuotationFlightPlans(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void apply(Quotation quotation, JsonNode input) {
        if (input == null) return;
        if (input.isNull()) throw new IllegalArgumentException("Envie as opções revisadas; o plano não pode ser removido com null.");
        if (!"BRL".equals(quotation.getCurrency())) throw new IllegalArgumentException("O cálculo de milhas usa reais (BRL).");
        ObjectNode normalized = normalize(input);
        for (JsonNode option : normalized.path("options")) {
            String importId = option.path("calculo").path("importacaoId").asText("");
            if (!importId.isEmpty()) {
                var sources = jdbc.queryForList("SELECT filename,created_at FROM flight_imports WHERE id=? AND agency_id=? AND status='DONE'", UUID.fromString(importId), quotation.getAgency().getId());
                if (sources.size() != 1) throw new IllegalArgumentException("Importação não encontrada nesta agência.");
                ObjectNode calculation = (ObjectNode) option.path("calculo");
                calculation.put("arquivoOrigem", (String) sources.get(0).get("filename"));
                calculation.put("importadoEm", sources.get(0).get("created_at").toString());
            }
        }
        normalized.put("savedAt", Instant.now().toString());
        normalized.put("savedBy", SecurityContextUsers.optionalUser().map(u -> u.getId().toString()).orElse(""));
        normalized.put("savedByName", SecurityContextUsers.optionalUser().map(u -> u.getName()).orElse(""));
        quotation.setFlightPlan(normalized);
        for (JsonNode option : normalized.path("options")) {
            if (option.path("id").asText().equals(normalized.path("selectedOptionId").asText()))
                quotation.setTotalAmount(option.path("precoTotal").decimalValue());
        }
    }

    public void record(Quotation q) {
        if (q.getFlightPlan() == null) return;
        jdbc.update("INSERT INTO quotation_flight_history(id,quotation_id,agency_id,saved_by,plan) VALUES (?,?,?,?,CAST(? AS jsonb))",
                UUID.randomUUID(), q.getId(), q.getAgency().getId(),
                SecurityContextUsers.optionalUser().map(u -> u.getId()).orElse(null), q.getFlightPlan().toString());
    }

    public static ObjectNode normalize(JsonNode input) {
        if (!input.isObject() || !input.path("options").isArray() || input.path("options").isEmpty() || input.path("options").size() > 20)
            throw new IllegalArgumentException("Informe de 1 a 20 opções de voo.");
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        String selected = string(input, "selectedOptionId", 100, true);
        result.put("selectedOptionId", selected);
        ArrayNode options = result.putArray("options");
        Set<String> ids = new HashSet<>();
        for (JsonNode raw : input.path("options")) {
            ObjectNode option = options.addObject();
            String id = string(raw, "id", 100, true);
            if (!ids.add(id)) throw new IllegalArgumentException("Opção de voo duplicada.");
            option.put("id", id);
            for (String field : List.of("nome", "cia", "horarioSaida", "horarioChegada", "conexoes")) option.put(field, string(raw, field, 150, field.equals("nome")));
            String color = string(raw, "corCia", 20, false);
            if (color.matches("#[0-9a-fA-F]{6}")) option.put("corCia", color);
            int passengers = integer(raw, "qtdPessoas", 1, 20);
            option.put("qtdPessoas", passengers);
            if (!raw.path("segmentos").isArray() || raw.path("segmentos").isEmpty() || raw.path("segmentos").size() > 8)
                throw new IllegalArgumentException("Informe os trechos da opção.");
            ArrayNode segments = option.putArray("segmentos");
            for (JsonNode s : raw.path("segmentos")) {
                ObjectNode segment = segments.addObject();
                for (String field : List.of("origin", "destination")) segment.put(field, string(s, field, 100, false));
                for (String field : List.of("departureDate", "arrivalDate")) {
                    String date = string(s, field, 10, false);
                    if (date.isBlank()) { segment.putNull(field); continue; }
                    try {
                        segment.put(field, LocalDate.parse(date).toString());
                    } catch (java.time.format.DateTimeParseException ex) {
                        throw new IllegalArgumentException("Data inválida: " + field);
                    }
                }
                if (!segment.path("arrivalDate").isNull() && !segment.path("departureDate").isNull() && LocalDate.parse(segment.path("arrivalDate").asText()).isBefore(LocalDate.parse(segment.path("departureDate").asText())))
                    throw new IllegalArgumentException("A chegada não pode ser anterior à data de saída.");
                for (String field : List.of("departureTime", "arrivalTime")) {
                    String time = string(s, field, 5, false);
                    if (time.isBlank()) { segment.putNull(field); continue; }
                    if (!time.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]")) throw new IllegalArgumentException("Horário inválido.");
                    LocalTime.parse(time); segment.put(field, time);
                }
                if (s.path("durationMinutes").isNull() || s.path("durationMinutes").isMissingNode()) segment.putNull("durationMinutes");
                else segment.put("durationMinutes", integer(s, "durationMinutes", 1, 10080));
                if (s.path("stops").isNull() || s.path("stops").isMissingNode()) segment.putNull("stops");
                else segment.put("stops", integer(s, "stops", 0, 20));
            }
            JsonNode source = raw.path("calculo");
            ObjectNode calculation = option.putObject("calculo");
            if (!source.path("revisado").asBoolean()) throw new IllegalArgumentException("Confira os dados e confirme a revisão da opção.");
            calculation.put("revisado", true);
            String type = string(source, "tipo", 20, true);
            if (!List.of("so_ida", "ida_volta", "preco_unico").contains(type)) throw new IllegalArgumentException("Tipo de trecho inválido.");
            calculation.put("tipo", type);
            BigDecimal milesOut = number(source, "milhasIda", 100_000_000);
            BigDecimal milesBack = number(source, "milhasVolta", 100_000_000);
            if (milesOut.stripTrailingZeros().scale() > 0 || milesBack.stripTrailingZeros().scale() > 0) throw new IllegalArgumentException("Milhas devem ser inteiras.");
            BigDecimal miles = milesOut.add(type.equals("ida_volta") ? milesBack : BigDecimal.ZERO);
            BigDecimal thousand = number(source, "custoPorMilheiro", 100000);
            if (miles.signum() > 0 && thousand.signum() <= 0) throw new IllegalArgumentException("Informe o custo do milheiro para calcular as milhas preenchidas, ou deixe as milhas vazias.");
            calculation.put("milhasIda", milesOut); calculation.put("milhasVolta", milesBack); calculation.put("custoPorMilheiro", thousand);
            BigDecimal fees = money(number(source, "taxas", 10000000));
            BigDecimal extra = money(number(source, "taxasAdicionais", 10000000));
            BigDecimal cost = money(miles.multiply(thousand).divide(new BigDecimal("1000")));
            BigDecimal base = money(cost.add(fees).add(extra));
            JsonNode rawProfit = source.path("lucroConfig");
            ObjectNode profitConfig = calculation.putObject("lucroConfig");
            BigDecimal pct = number(rawProfit, "pct", 1000), fixed = money(number(rawProfit, "fixo", 10000000));
            boolean usePct = rawProfit.path("usarPct").asBoolean(), useFixed = rawProfit.path("usarFixo").asBoolean();
            profitConfig.put("usarPct", usePct); profitConfig.put("usarFixo", useFixed); profitConfig.put("pct", pct); profitConfig.put("fixo", fixed);
            BigDecimal profit = money((usePct ? base.multiply(pct).divide(new BigDecimal("100")) : BigDecimal.ZERO).add(useFixed ? fixed : BigDecimal.ZERO));
            BigDecimal bagPrice = money(number(source, "valorMala", 100000));
            int bags = integer(source, "qtdMalas", 0, 100);
            BigDecimal baggage = money(bagPrice.multiply(BigDecimal.valueOf(bags)));
            BigDecimal tickets = money(base.add(profit).multiply(BigDecimal.valueOf(passengers)));
            calculation.put("taxas", fees); calculation.put("taxasAdicionais", extra); calculation.put("custoMilhas", cost);
            calculation.put("basePorPessoa", base); calculation.put("lucroPorPessoa", profit);
            calculation.put("valorMala", bagPrice); calculation.put("qtdMalas", bags);
            String importId = string(source, "importacaoId", 36, false);
            if (!importId.isBlank()) { UUID.fromString(importId); calculation.put("importacaoId", importId); }
            option.put("precoPassagens", tickets); option.put("precoBagagens", baggage); option.put("precoTotal", tickets.add(baggage));
        }
        if (!ids.contains(selected)) throw new IllegalArgumentException("Selecione a opção que define o total da cotação.");
        return result;
    }
    private static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private static String string(JsonNode node, String key, int max, boolean required) {
        JsonNode value = node.path(key);
        if (!value.isMissingNode() && !value.isNull() && !value.isTextual()) throw new IllegalArgumentException("Campo inválido: " + key);
        String s = value.asText("").strip();
        if (s.length() > max || required && s.isEmpty()) throw new IllegalArgumentException("Confira o campo " + key);
        return s;
    }
    private static BigDecimal number(JsonNode node, String key, int max) {
        JsonNode value = node.path(key);
        if (!value.isNumber()) throw new IllegalArgumentException("Informe o campo " + key);
        BigDecimal n = value.decimalValue();
        if (n.signum() < 0 || n.compareTo(BigDecimal.valueOf(max)) > 0 || n.stripTrailingZeros().scale() > 2) throw new IllegalArgumentException("Valor inválido: " + key);
        return n;
    }
    private static int integer(JsonNode node, String key, int min, int max) {
        BigDecimal n = number(node, key, max);
        if (n.compareTo(BigDecimal.valueOf(min)) < 0 || n.stripTrailingZeros().scale() > 0) throw new IllegalArgumentException("Valor inválido: " + key);
        return n.intValueExact();
    }
}
