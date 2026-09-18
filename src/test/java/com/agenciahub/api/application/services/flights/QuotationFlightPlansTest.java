package com.agenciahub.api.application.services.flights;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class QuotationFlightPlansTest {
    static ObjectNode plan() throws Exception {
        return (ObjectNode) new ObjectMapper().readTree("""
            {"selectedOptionId":"outbound","options":[{
              "id":"outbound","nome":"Voo","cia":"LATAM","qtdPessoas":2,"precoTotal":1,
              "segmentos":[{"origin":"BPS","destination":"REC","departureDate":"2026-09-20",
                "arrivalDate":"2026-09-20","departureTime":"17:25","arrivalTime":"23:20","durationMinutes":355,"stops":1}],
              "calculo":{"tipo":"so_ida","milhasIda":20000,"milhasVolta":0,"custoPorMilheiro":25,
                "taxas":50,"taxasAdicionais":0,"valorMala":0,"qtdMalas":0,"revisado":true,
                "lucroConfig":{"usarPct":true,"pct":10,"usarFixo":true,"fixo":20}}
            }]}
            """);
    }
    @Test void recalculatesInsteadOfTrustingClientPrice() throws Exception {
        var normalized = QuotationFlightPlans.normalize(plan());
        assertThat(normalized.path("options").get(0).path("precoTotal").decimalValue()).isEqualByComparingTo("1250.00");
        assertThat(normalized.path("options").get(0).path("calculo").path("lucroPorPessoa").decimalValue()).isEqualByComparingTo("75.00");
    }
    @Test void missingReviewAndUnknownSelectedOptionAreRejected() throws Exception {
        var input = plan(); ((ObjectNode) input.path("options").get(0).path("calculo")).put("revisado", false);
        assertThatThrownBy(() -> QuotationFlightPlans.normalize(input)).isInstanceOf(IllegalArgumentException.class);
        var other = plan(); other.put("selectedOptionId", "not-in-options");
        assertThatThrownBy(() -> QuotationFlightPlans.normalize(other)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void missingDateDoesNotBecomeToday() throws Exception {
        var input = plan(); ((ObjectNode) input.path("options").get(0).path("segmentos").get(0)).putNull("departureDate");
        assertThat(QuotationFlightPlans.normalize(input).path("options").get(0).path("segmentos").get(0).path("departureDate").isNull()).isTrue();
    }
    @Test void syntheticLatamExampleUsesSameRoundingAsCalculator() throws Exception {
        var input = plan(); ObjectNode option = (ObjectNode) input.path("options").get(0);
        option.put("qtdPessoas", 1); ObjectNode calculation = (ObjectNode) option.path("calculo");
        calculation.put("milhasIda", 87141); calculation.put("taxas", 38.84);
        ((ObjectNode) calculation.path("lucroConfig")).put("usarFixo", false);
        assertThat(QuotationFlightPlans.normalize(input).path("options").get(0).path("precoTotal").decimalValue()).isEqualByComparingTo("2439.11");
    }
    @Test void halfCentRoundsUpBeforeProfit() throws Exception {
        var input = plan(); ObjectNode option = (ObjectNode) input.path("options").get(0);
        option.put("qtdPessoas", 1); ObjectNode calculation = (ObjectNode) option.path("calculo");
        calculation.put("milhasIda", 1005); calculation.put("custoPorMilheiro", 1); calculation.put("taxas", 0);
        ((ObjectNode) calculation.path("lucroConfig")).put("usarPct", false).put("usarFixo", false);
        assertThat(QuotationFlightPlans.normalize(input).path("options").get(0).path("precoTotal").decimalValue()).isEqualByComparingTo("1.01");
    }
    @Test void invalidDateAndTimeAreClientErrors() throws Exception {
        var input = plan(); ObjectNode segment = (ObjectNode) input.path("options").get(0).path("segmentos").get(0);
        segment.put("departureDate", "2026-02-30");
        assertThatThrownBy(() -> QuotationFlightPlans.normalize(input)).isInstanceOf(IllegalArgumentException.class);
        segment.put("departureDate", "2026-09-20").put("departureTime", "29:00");
        assertThatThrownBy(() -> QuotationFlightPlans.normalize(input)).isInstanceOf(IllegalArgumentException.class);
    }
}
