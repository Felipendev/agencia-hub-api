package com.agenciahub.api.dto.financial;

import com.agenciahub.api.domain.FinancialEntryCategory;
import com.agenciahub.api.domain.FinancialEntryStatus;
import com.agenciahub.api.domain.FinancialEntryType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Partial update: {@code null} fields are ignored.
 */
public record UpdateFinancialEntryRequest(
        String description,
        FinancialEntryType type,
        FinancialEntryCategory category,
        BigDecimal amount,
        LocalDate entryDate,
        FinancialEntryStatus status,
        UUID customerId,
        String bankAccount
) {
}
