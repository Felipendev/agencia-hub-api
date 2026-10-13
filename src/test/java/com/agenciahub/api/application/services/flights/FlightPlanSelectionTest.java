package com.agenciahub.api.application.services.flights;

import com.agenciahub.api.application.persistence.entity.Quotation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FlightPlanSelectionTest {
    @Test void reproducesReportedFixedProfitAndSelectedTravelDates() throws Exception {
        ObjectNode plan=QuotationFlightPlansTest.plan();
        ObjectNode option=(ObjectNode)plan.path("options").get(0);
        option.put("qtdPessoas",1);
        ObjectNode calc=(ObjectNode)option.path("calculo");
        calc.put("milhasIda",0).put("taxas",1162.99).put("fonteTipo","CONSOLIDADORA").put("empresaVendedora","Pátria").put("observacoesComparativo","5x no cartão");
        ((ObjectNode)calc.path("lucroConfig")).put("usarPct",false).put("usarFixo",true).put("fixo",170);
        Quotation q=new Quotation();q.setFlightPlan(QuotationFlightPlans.normalize(plan));
        assertThat(FlightPlanSelection.cost(q)).isEqualByComparingTo("1162.99");
        assertThat(FlightPlanSelection.option(q).path("precoTotal").decimalValue()).isEqualByComparingTo("1332.99");
        assertThat(FlightPlanSelection.start(q)).isEqualTo(java.time.LocalDate.of(2026,9,20));
        assertThat(FlightPlanSelection.option(q).path("calculo").path("empresaVendedora").asText()).isEqualTo("Pátria");
        assertThat(FlightPlanSelection.option(q).path("calculo").path("observacoesComparativo").asText()).isEqualTo("5x no cartão");
    }
    @Test void unknownCostRemainsUnknown() { assertThat(FlightPlanSelection.cost(new Quotation())).isNull(); }
}
