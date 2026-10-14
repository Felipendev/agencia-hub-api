package com.agenciahub.api.domain.savings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Economia realizada de uma viagem (produto Viagens): preço comparável equivalente menos o custo efetivo do cliente.
 * Não é lucro da agência nem estimativa de cotação. Sem base documentada, o resultado é indisponível.
 */
public final class TripSavings {

    private TripSavings() {}

    public static final ZoneId AGENCY_ZONE = ZoneId.of("America/Fortaleza");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000000000000000");
    private static final int MAX_LINES = 30;

    public static LocalDate today() { return LocalDate.now(AGENCY_ZONE); }

    public enum SourceType { AIRLINE_SITE, ONLINE_TRAVEL_AGENCY, METASEARCH, SUPPLIER_QUOTE, OTHER }

    /** Quem recebeu o pagamento ou a natureza do custo; taxas e honorários são linhas próprias. */
    public enum CostKind { AGENCY_PAYMENT, DIRECT_SUPPLIER_PAYMENT, TAXES, SERVICE_FEE, MILES_ACQUISITION, OTHER }

    public enum AdjustmentKind {
        REFUND(true), OTHER_CREDIT(true), CANCELLATION_FEE(false), CHANGE_FEE(false), OTHER_CHARGE(false);

        private final boolean credit;

        AdjustmentKind(boolean credit) { this.credit = credit; }

        public boolean credit() { return credit; }
    }

    /** Uso de milhas na emissão. {@code UNKNOWN} torna a economia indisponível: o custo não é conhecido. */
    public enum MilesUsage { NOT_USED, KNOWN, UNKNOWN }

    public enum Unavailable {
        NO_ANALYSIS("Sem análise de economia registrada"),
        MISSING_COMPARABLE_PRICE("Falta o preço comparável"),
        MISSING_SOURCE("Falta a fonte do preço comparável"),
        MISSING_RESEARCH_DATE("Falta a data da pesquisa"),
        NOT_EQUIVALENT("Comparação não confirmada como equivalente (rota, datas, passageiros e condições)"),
        MISSING_COSTS("Falta o custo efetivo da viagem"),
        MILES_USAGE_NOT_INFORMED("Falta informar se a emissão usou milhas"),
        MILES_COST_UNKNOWN("Custo de aquisição das milhas desconhecido"),
        MISSING_METHODOLOGY("Falta a metodologia"),
        TRIP_CANCELLED("Viagem cancelada: não há economia realizada");

        private final String label;

        Unavailable(String label) { this.label = label; }

        public String label() { return label; }
    }

    public record Comparison(
            BigDecimal price,
            SourceType sourceType,
            String sourceDescription,
            String sourceUrl,
            LocalDate researchedOn,
            String route,
            LocalDate outboundDate,
            LocalDate returnDate,
            Integer passengers,
            String fareConditions,
            String baggage,
            Boolean sameRoute,
            Boolean sameDates,
            Boolean samePassengers,
            Boolean equivalentConditions,
            String differences) {

        boolean equivalent() {
            return Boolean.TRUE.equals(sameRoute) && Boolean.TRUE.equals(sameDates)
                    && Boolean.TRUE.equals(samePassengers) && Boolean.TRUE.equals(equivalentConditions);
        }
    }

    public record CostLine(CostKind kind, BigDecimal amount, String description, String origin, Long miles) {}

    public record Adjustment(AdjustmentKind kind, BigDecimal amount, LocalDate date, String description) {}

    public record Data(Comparison comparison, List<CostLine> costs, MilesUsage milesUsage, List<Adjustment> adjustments,
                       String methodology, String notes) {

        public List<CostLine> costs() { return costs == null ? List.of() : costs; }

        public List<Adjustment> adjustments() { return adjustments == null ? List.of() : adjustments; }
    }

    public record Reason(Unavailable code, String label) {
        static Reason of(Unavailable code) { return new Reason(code, code.label()); }
    }

    /** Valores sempre calculados pela API; {@code savings} só existe quando {@code available}. */
    public record Result(boolean available, List<Reason> reasons, BigDecimal comparablePrice, BigDecimal baseCost,
                         BigDecimal adjustments, BigDecimal effectiveCost, BigDecimal savings, BigDecimal savingsPercent) {

        public static Result noAnalysis(boolean cancelled) {
            List<Reason> reasons = new ArrayList<>(List.of(Reason.of(Unavailable.NO_ANALYSIS)));
            if (cancelled) reasons.add(Reason.of(Unavailable.TRIP_CANCELLED));
            return new Result(false, List.copyOf(reasons), null, null, null, null, null, null);
        }
    }

    /** Valida estrutura e coerência; campos ausentes não são erro — tornam o resultado indisponível. */
    public static void validate(Data data) {
        if (data == null) invalid("Informe os dados da análise.");
        Comparison c = data.comparison();
        if (c != null) {
            amount(c.price(), "O preço comparável", true);
            maxLength(c.sourceDescription(), 500, "A descrição da fonte");
            maxLength(c.route(), 300, "A rota");
            maxLength(c.fareConditions(), 300, "As condições da tarifa");
            maxLength(c.baggage(), 300, "A bagagem");
            maxLength(c.differences(), 1000, "As diferenças");
            if (c.sourceUrl() != null && !c.sourceUrl().isBlank()) url(c.sourceUrl());
            if (c.researchedOn() != null && c.researchedOn().isAfter(today())) invalid("A data da pesquisa não pode ser futura.");
            if (c.outboundDate() != null && c.returnDate() != null && c.returnDate().isBefore(c.outboundDate())) {
                invalid("A data de volta do comparativo não pode ser anterior à ida.");
            }
            if (c.passengers() != null && (c.passengers() < 1 || c.passengers() > 99)) invalid("Informe de 1 a 99 passageiros.");
        }
        if (data.costs().size() > MAX_LINES || data.adjustments().size() > MAX_LINES) invalid("Use no máximo 30 linhas de custo e 30 ajustes.");
        boolean milesLine = false;
        for (CostLine line : data.costs()) {
            if (line == null || line.kind() == null) invalid("Informe o tipo de cada custo.");
            amount(line.amount(), "O valor do custo", false);
            if (line.amount() == null) invalid("Informe o valor de cada custo.");
            required(line.description(), 300, "A descrição do custo");
            maxLength(line.origin(), 300, "A origem do custo");
            if (line.kind() == CostKind.MILES_ACQUISITION) {
                milesLine = true;
                if (line.origin() == null || line.origin().isBlank()) {
                    invalid("Informe a origem do custo de aquisição das milhas (ex.: compra, transferência, clube).");
                }
                if (line.miles() != null && (line.miles() < 1 || line.miles() > 1_000_000_000L)) invalid("Quantidade de milhas inválida.");
            }
        }
        if (milesLine && data.milesUsage() != MilesUsage.KNOWN) {
            invalid("Há custo de aquisição de milhas: marque que a emissão usou milhas com custo conhecido.");
        }
        if (data.milesUsage() == MilesUsage.KNOWN && !milesLine) {
            invalid("Com custo de milhas conhecido, inclua a linha de aquisição das milhas com a origem.");
        }
        for (Adjustment adjustment : data.adjustments()) {
            if (adjustment == null || adjustment.kind() == null) invalid("Informe o tipo de cada ajuste.");
            amount(adjustment.amount(), "O valor do ajuste", true);
            if (adjustment.amount() == null) invalid("Informe o valor de cada ajuste.");
            if (adjustment.date() == null) invalid("Informe a data de cada ajuste.");
            if (adjustment.date().isAfter(today())) invalid("A data do ajuste não pode ser futura.");
            required(adjustment.description(), 300, "A descrição do ajuste");
        }
        maxLength(data.methodology(), 4000, "A metodologia");
        maxLength(data.notes(), 2000, "As observações");
    }

    public static Result compute(Data data, boolean tripCancelled) {
        Set<Unavailable> reasons = EnumSet.noneOf(Unavailable.class);
        Comparison c = data.comparison();
        if (c == null || c.price() == null) reasons.add(Unavailable.MISSING_COMPARABLE_PRICE);
        if (c == null || c.sourceType() == null || blank(c.sourceDescription())) reasons.add(Unavailable.MISSING_SOURCE);
        if (c == null || c.researchedOn() == null) reasons.add(Unavailable.MISSING_RESEARCH_DATE);
        if (c == null || !c.equivalent()) reasons.add(Unavailable.NOT_EQUIVALENT);
        if (data.costs().isEmpty()) reasons.add(Unavailable.MISSING_COSTS);
        if (data.milesUsage() == null) reasons.add(Unavailable.MILES_USAGE_NOT_INFORMED);
        if (data.milesUsage() == MilesUsage.UNKNOWN) reasons.add(Unavailable.MILES_COST_UNKNOWN);
        if (blank(data.methodology())) reasons.add(Unavailable.MISSING_METHODOLOGY);
        if (tripCancelled) reasons.add(Unavailable.TRIP_CANCELLED);

        BigDecimal baseCost = data.costs().stream().map(CostLine::amount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal adjustments = data.adjustments().stream()
                .filter(a -> a.amount() != null && a.kind() != null)
                .map(a -> a.kind().credit() ? a.amount().negate() : a.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal effectiveCost = baseCost.add(adjustments);
        BigDecimal price = c == null || c.price() == null ? null : c.price().setScale(2, RoundingMode.UNNECESSARY);
        List<Reason> reasonList = reasons.stream().map(Reason::of).toList();
        if (!reasons.isEmpty()) {
            return new Result(false, reasonList, price, baseCost, adjustments, effectiveCost, null, null);
        }
        BigDecimal savings = price.subtract(effectiveCost);
        BigDecimal percent = savings.multiply(BigDecimal.valueOf(100)).divide(price, 1, RoundingMode.HALF_UP);
        return new Result(true, List.of(), price, baseCost, adjustments, effectiveCost, savings, percent);
    }

    private static void amount(BigDecimal value, String name, boolean positive) {
        if (value == null) return;
        if (value.scale() > 2) invalid(name + " deve ter no máximo 2 casas decimais.");
        if (positive ? value.signum() <= 0 : value.signum() < 0) invalid(name + (positive ? " deve ser maior que zero." : " não pode ser negativo."));
        if (value.compareTo(MAX_AMOUNT) >= 0) invalid(name + " é muito alto.");
    }

    private static void url(String raw) {
        try {
            URI uri = URI.create(raw.strip());
            String scheme = Objects.toString(uri.getScheme(), "").toLowerCase(java.util.Locale.ROOT);
            if (!Set.of("http", "https").contains(scheme) || uri.getHost() == null || uri.getUserInfo() != null || raw.length() > 2048) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException e) {
            invalid("Informe o link da fonte começando com https:// ou http://.");
        }
    }

    private static void required(String value, int max, String name) {
        if (blank(value)) invalid(name + " é obrigatória.");
        maxLength(value, max, name);
    }

    private static void maxLength(String value, int max, String name) {
        if (value != null && value.length() > max) invalid(name + " pode ter até " + max + " caracteres.");
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    public static void invalid(String message) { throw new IllegalArgumentException(message); }
}
