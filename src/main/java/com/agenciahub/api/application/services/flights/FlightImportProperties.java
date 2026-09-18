package com.agenciahub.api.application.services.flights;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Component
@Validated
@ConfigurationProperties(prefix = "flight-import")
@Getter @Setter
public class FlightImportProperties {
    private String apiKey = "";
    @NotBlank @Size(max = 80) @Pattern(regexp = "[a-zA-Z0-9.-]+")
    private String model = "gemini-3.1-flash-lite";
    @Min(0) @Max(1000000)
    private int monthlyRequests = 1000;
    @NotNull @DecimalMin("0")
    private BigDecimal monthlyBudgetUsd = new BigDecimal("5.00");
    // Conservative charge retained on ambiguous failures (a timed-out call may be billed).
    @NotNull @DecimalMin("0.10")
    private BigDecimal reservationUsd = new BigDecimal("0.10");
    @NotNull @DecimalMin("0")
    private BigDecimal inputUsdPerMillion = new BigDecimal("0.25");
    @NotNull @DecimalMin("0")
    private BigDecimal outputUsdPerMillion = new BigDecimal("1.50");
}
