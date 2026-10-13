package com.agenciahub.api.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public final class AccumulationSimulation {
    private AccumulationSimulation() {}
    public record Scenario(@NotBlank @Size(max=100) String program,
                           @NotNull @DecimalMin("0") @DecimalMax("1000") BigDecimal bonusPercent) {}
    public record Assumptions(
        @NotBlank @Size(max=200) String title,
        @NotNull @DecimalMin("0") @DecimalMax("10000000") BigDecimal monthlySpending,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal pointsPerDollar,
        @NotNull @DecimalMin("0.01") @DecimalMax("1000000") BigDecimal exchangeRate,
        @NotNull @PastOrPresent LocalDate exchangeDate,
        @Min(1) @Max(120) int months,
        @Min(1) @Max(99) int passengers,
        @Min(1) @Max(1000000000) long milesPerPassenger,
        @NotBlank @Size(max=200) String destination,
        @NotNull @Size(max=10) List<@NotNull @Valid Scenario> scenarios) {}
    public record Projection(String program, BigDecimal bonusPercent, long projectedMiles, long remainingMiles, boolean goalReached) {}
    public record Result(BigDecimal monthlyPoints, long basePoints, long goalMiles, List<Projection> scenarios) {}

    public static Result calculate(Assumptions input) {
        BigDecimal numerator=input.monthlySpending().multiply(input.pointsPerDollar());
        BigDecimal monthly=numerator.divide(input.exchangeRate(),2,RoundingMode.HALF_UP);
        long base=numerator.multiply(BigDecimal.valueOf(input.months())).divide(input.exchangeRate(),0,RoundingMode.DOWN).longValueExact();
        long goal=Math.multiplyExact(input.milesPerPassenger(),input.passengers());
        List<Projection> projections=input.scenarios().stream().map(s -> {
            long miles=BigDecimal.valueOf(base).multiply(BigDecimal.ONE.add(s.bonusPercent().movePointLeft(2))).setScale(0,RoundingMode.DOWN).longValueExact();
            return new Projection(s.program(),s.bonusPercent(),miles,Math.max(0,goal-miles),miles>=goal);
        }).toList();
        return new Result(monthly,base,goal,projections);
    }
}
