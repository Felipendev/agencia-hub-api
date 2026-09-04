package com.agenciahub.api.application.usecases.financial.create;

import com.agenciahub.api.domain.FinancialEntryCategory;
import com.agenciahub.api.domain.FinancialEntryStatus;
import com.agenciahub.api.domain.FinancialEntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateFinancialEntryRequestDTO(
        @NotBlank String description,
        @NotNull FinancialEntryType type,
        @NotNull FinancialEntryCategory category,
        @NotNull BigDecimal amount,
        @NotNull LocalDate entryDate,
        @NotNull FinancialEntryStatus status,
        UUID customerId,
        UUID supplierId,
        String bankAccount,
        String recurrenceFrequency,
        String notes,
        BigDecimal saleAmount,
        BigDecimal supplierCost,
        BigDecimal commissionAmount
) {
}
