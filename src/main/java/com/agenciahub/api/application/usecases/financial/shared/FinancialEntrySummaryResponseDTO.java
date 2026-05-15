package com.agenciahub.api.application.usecases.financial.shared;

import com.agenciahub.api.domain.FinancialEntryCategory;
import com.agenciahub.api.domain.FinancialEntryStatus;
import com.agenciahub.api.domain.FinancialEntryType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FinancialEntrySummaryResponseDTO(
        UUID id,
        String description,
        FinancialEntryType type,
        FinancialEntryCategory category,
        BigDecimal amount,
        LocalDate entryDate,
        FinancialEntryStatus status,
        UUID customerId,
        String customerName,
        String bankAccount
) {
}
