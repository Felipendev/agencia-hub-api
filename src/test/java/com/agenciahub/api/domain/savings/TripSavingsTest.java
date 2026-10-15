package com.agenciahub.api.domain.savings;

import com.agenciahub.api.domain.savings.TripSavings.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TripSavingsTest {

    private static Comparison equivalent(String price) {
        return new Comparison(new BigDecimal(price), SourceType.AIRLINE_SITE, "Site da companhia", "https://example.com/busca",
                LocalDate.of(2026, 9, 1), "FOR-LIS", LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 15), 2,
                "Econômica", "1 mala de 23 kg", true, true, true, true, null);
    }

    private static final List<CostLine> COSTS = List.of(
            new CostLine(CostKind.AGENCY_PAYMENT, new BigDecimal("3000.00"), "Pago à agência", null, null),
            new CostLine(CostKind.TAXES, new BigDecimal("400.00"), "Taxas de embarque", null, null),
            new CostLine(CostKind.SERVICE_FEE, new BigDecimal("150.50"), "Honorários", null, null));

    @Test
    void savingsIsComparablePriceMinusEffectiveCostIncludingFeesAndAdjustments() {
        var data = new Data(equivalent("5000.00"), COSTS, MilesUsage.NOT_USED, List.of(
                new Adjustment(AdjustmentKind.REFUND, new BigDecimal("100.00"), LocalDate.of(2026, 10, 1), "Reembolso de taxa"),
                new Adjustment(AdjustmentKind.CHANGE_FEE, new BigDecimal("50.00"), LocalDate.of(2026, 10, 2), "Remarcação")),
                "Mesma rota e datas pesquisadas no site da companhia", null);
        TripSavings.validate(data);
        var result = TripSavings.compute(data, false);
        assertThat(result.available()).isTrue();
        assertThat(result.baseCost()).isEqualByComparingTo("3550.50");
        assertThat(result.adjustments()).isEqualByComparingTo("-50.00");
        assertThat(result.effectiveCost()).isEqualByComparingTo("3500.50");
        assertThat(result.savings()).isEqualByComparingTo("1499.50");
        assertThat(result.savingsPercent()).isEqualByComparingTo("30.0");
    }

    @Test
    void negativeResultIsReportedAsIsWithoutClamping() {
        var data = new Data(equivalent("3000.00"), COSTS, MilesUsage.NOT_USED, List.of(), "m", null);
        assertThat(TripSavings.compute(data, false).savings()).isEqualByComparingTo("-550.50");
    }

    @Test
    void undocumentedBasesNeverProduceANumber() {
        var notEquivalent = new Comparison(new BigDecimal("5000"), SourceType.OTHER, "Busca", null, LocalDate.of(2026, 9, 1),
                null, null, null, 1, null, null, true, false, true, true, "Datas diferentes");
        var result = TripSavings.compute(new Data(notEquivalent, COSTS, MilesUsage.NOT_USED, null, "m", null), false);
        assertThat(result.available()).isFalse();
        assertThat(result.savings()).isNull();
        assertThat(result.reasons()).extracting(Reason::code).contains(Unavailable.NOT_EQUIVALENT, Unavailable.MISSING_COMPARISON_DETAILS);

        var unknownMiles = TripSavings.compute(new Data(equivalent("5000"), COSTS, MilesUsage.UNKNOWN, null, "m", null), false);
        assertThat(unknownMiles.reasons()).extracting(Reason::code).containsExactly(Unavailable.MILES_COST_UNKNOWN);

        var empty = TripSavings.compute(new Data(null, null, null, null, null, null), false);
        assertThat(empty.reasons()).extracting(Reason::code).contains(Unavailable.MISSING_COMPARABLE_PRICE, Unavailable.MISSING_SOURCE,
                Unavailable.MISSING_RESEARCH_DATE, Unavailable.NOT_EQUIVALENT, Unavailable.MISSING_COSTS,
                Unavailable.MILES_USAGE_NOT_INFORMED, Unavailable.MISSING_METHODOLOGY);

        var cancelled = TripSavings.compute(new Data(equivalent("5000"), COSTS, MilesUsage.NOT_USED, null, "m", null), true);
        assertThat(cancelled.available()).isFalse();
        assertThat(cancelled.reasons()).extracting(Reason::code).containsExactly(Unavailable.TRIP_CANCELLED);
    }

    @Test
    void milesCostRequiresOriginAndConsistentUsage() {
        var withoutOrigin = new Data(equivalent("5000"), List.of(new CostLine(CostKind.MILES_ACQUISITION, new BigDecimal("900"), "50 mil milhas", " ", 50_000L)),
                MilesUsage.KNOWN, null, "m", null);
        assertThatThrownBy(() -> TripSavings.validate(withoutOrigin)).hasMessageContaining("origem");
        var inconsistent = new Data(equivalent("5000"), List.of(new CostLine(CostKind.MILES_ACQUISITION, new BigDecimal("900"), "Milhas", "Compra", 50_000L)),
                MilesUsage.NOT_USED, null, "m", null);
        assertThatThrownBy(() -> TripSavings.validate(inconsistent)).hasMessageContaining("usou milhas");
        assertThatThrownBy(() -> TripSavings.validate(new Data(equivalent("5000"), COSTS, MilesUsage.KNOWN, null, "m", null)))
                .hasMessageContaining("linha de aquisição");
        var known = new Data(equivalent("5000"), List.of(new CostLine(CostKind.MILES_ACQUISITION, new BigDecimal("900"), "Milhas", "Compra no clube", 50_000L)),
                MilesUsage.KNOWN, null, "m", null);
        TripSavings.validate(known);
        assertThat(TripSavings.compute(known, false).savings()).isEqualByComparingTo("4100.00");
    }

    @Test
    void confirmationsWithoutDocumentedComparisonDetailsNeverProduceSavings() {
        Comparison incomplete = new Comparison(new BigDecimal("5000"), SourceType.OTHER, "Pesquisa", null,
                TripSavings.today(), " ", null, null, null, "", "", true, true, true, true, null);
        Result result = TripSavings.compute(new Data(incomplete, COSTS, MilesUsage.NOT_USED, null, "Mesmas condições", null), false);
        assertThat(result.available()).isFalse();
        assertThat(result.savings()).isNull();
        assertThat(result.reasons()).extracting(Reason::code).contains(Unavailable.MISSING_COMPARISON_DETAILS);
    }

    @Test
    void rejectsInvalidAmountsDatesAndLinks() {
        assertThatThrownBy(() -> TripSavings.validate(new Data(equivalent("0"), null, null, null, null, null))).hasMessageContaining("maior que zero");
        assertThatThrownBy(() -> TripSavings.validate(new Data(null, List.of(new CostLine(CostKind.TAXES, new BigDecimal("1.234"), "x", null, null)), null, null, null, null)))
                .hasMessageContaining("2 casas");
        assertThatThrownBy(() -> TripSavings.validate(new Data(null, List.of(new CostLine(CostKind.TAXES, new BigDecimal("-1"), "x", null, null)), null, null, null, null)))
                .hasMessageContaining("negativo");
        var future = new Comparison(new BigDecimal("1"), SourceType.OTHER, "x", null, TripSavings.today().plusDays(1), null, null, null, null, null, null, null, null, null, null, null);
        assertThatThrownBy(() -> TripSavings.validate(new Data(future, null, null, null, null, null))).hasMessageContaining("futura");
        var badUrl = new Comparison(new BigDecimal("1"), SourceType.OTHER, "x", "javascript:alert(1)", null, null, null, null, null, null, null, null, null, null, null, null);
        assertThatThrownBy(() -> TripSavings.validate(new Data(badUrl, null, null, null, null, null))).hasMessageContaining("https://");
        var futureAdjustment = new Adjustment(AdjustmentKind.REFUND, new BigDecimal("1"), TripSavings.today().plusDays(1), "x");
        assertThatThrownBy(() -> TripSavings.validate(new Data(null, null, null, List.of(futureAdjustment), null, null))).hasMessageContaining("futura");
    }
}
