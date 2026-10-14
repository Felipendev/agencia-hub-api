package com.agenciahub.api.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Manual declarations and projections; never logs into a loyalty program. */
public final class MilesWallet {
    private MilesWallet() {}
    public enum Kind { ACCOUNT, CARD, CLUB, EXPIRY }
    public record Profile(@Size(max=2000) String goals, @Size(max=200) String travelFrequency,
                          @Size(max=2000) String destinations, @Size(max=4000) String actionPlan) {}
    public record ProfileInput(@NotNull @Valid Profile data, @Min(0) int expectedVersion) {}
    public record ProfileRow(Profile data, int version, boolean archived) {}
    public record Data(
        @Size(max=120) String program, @Size(max=200) String holder, @Size(max=500) String source,
        LocalDate declaredOn, @JsonDeserialize(using=WholePointsDeserializer.class) @Min(0) @Max(1000000000) Long openingBalance,
        @Size(max=120) String issuer, @Size(max=120) String category,
        @DecimalMin("0") @DecimalMax("100") BigDecimal pointsPerDollar,
        @DecimalMin("0") @DecimalMax("10000000") BigDecimal monthlySpend,
        @JsonDeserialize(using=WholePointsDeserializer.class) @Min(1) @Max(31) Long creditDay,
        @DecimalMin("0") @DecimalMax("10000000") BigDecimal monthlyCost,
        @JsonDeserialize(using=WholePointsDeserializer.class) @Min(0) @Max(1000000000) Long monthlyPoints,
        UUID accountId, @JsonDeserialize(using=WholePointsDeserializer.class) @Min(1) @Max(1000000000) Long quantity, LocalDate expiresOn
    ) {
        public void validate(Kind kind) {
            switch(kind) {
                case ACCOUNT -> { required(program,"Programa"); required(holder,"Titular"); required(source,"Fonte do saldo");
                    if(declaredOn==null||declaredOn.isAfter(LocalDate.now())||openingBalance==null)invalid("Informe a data e o saldo inicial declarado."); }
                case CARD -> { required(issuer,"Emissor"); required(category,"Categoria"); }
                case CLUB -> { required(program,"Programa do clube"); if(monthlyCost==null||monthlyPoints==null)invalid("Informe custo e pontos mensais do clube."); }
                case EXPIRY -> { if(accountId==null||quantity==null||expiresOn==null)invalid("Informe a conta, quantidade e data do vencimento."); }
            }
        }
        private static void required(String value,String name) { if(value==null||value.isBlank())invalid(name+" é obrigatório."); }
    }
    public record ItemInput(@NotNull Kind kind, @NotNull @Valid Data data, @Min(0) int expectedVersion) {}
    public record Item(UUID id, Kind kind, Data data, int version, boolean archived, long balance) {}
    public record MovementInput(@NotNull UUID requestKey, @JsonDeserialize(using=WholePointsDeserializer.class) @NotNull @Min(-1000000000) @Max(1000000000) Long quantity,
                                @NotBlank @Size(max=500) String reason, @NotNull @PastOrPresent LocalDate occurredOn) {}
    public record ReversalInput(@NotBlank @Size(max=500) String reason) {}
    public record Movement(UUID id, UUID accountId, long quantity, String reason, LocalDate occurredOn,
                           UUID reversesId, String author, OffsetDateTime createdAt, boolean reversed) {}
    public record Audit(UUID id, UUID resourceId, String action, Object snapshot, String author, OffsetDateTime createdAt) {}
    public record View(ProfileRow profile, List<Item> items, List<Movement> movements, List<Audit> history) {}
    public static void invalid(String message) { throw new IllegalArgumentException(message); }
}
