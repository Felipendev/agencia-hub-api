package com.agenciahub.api.application.services.flights;

import com.agenciahub.api.application.persistence.entity.Quotation;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Uses the server-normalized selected option, never sums competing alternatives. */
public final class FlightPlanSelection {
    private FlightPlanSelection() {}
    public static JsonNode option(Quotation quotation) {
        JsonNode plan = quotation.getFlightPlan();
        if (plan == null) return null;
        for (JsonNode option : plan.path("options"))
            if (option.path("id").asText().equals(plan.path("selectedOptionId").asText())) return option;
        return null;
    }
    public static BigDecimal cost(Quotation quotation) {
        JsonNode option = option(quotation);
        if (option == null) return null;
        return option.path("calculo").path("basePorPessoa").decimalValue()
                .multiply(BigDecimal.valueOf(option.path("qtdPessoas").asInt(1)))
                .add(option.path("precoBagagens").decimalValue());
    }
    public static LocalDate start(Quotation quotation) {
        JsonNode option = option(quotation);
        if (option != null && !option.path("segmentos").isEmpty()) {
            String date = option.path("segmentos").get(0).path("departureDate").asText("");
            if (!date.isBlank()) return LocalDate.parse(date);
        }
        return quotation.getTravelStartDate();
    }
    public static LocalDate end(Quotation quotation) {
        JsonNode option = option(quotation);
        if (option != null && !option.path("segmentos").isEmpty()) {
            JsonNode last = option.path("segmentos").get(option.path("segmentos").size() - 1);
            String date = last.path("arrivalDate").asText("");
            if (date.isBlank()) date = last.path("departureDate").asText("");
            if (!date.isBlank()) return LocalDate.parse(date);
        }
        return quotation.getTravelEndDate();
    }
}
