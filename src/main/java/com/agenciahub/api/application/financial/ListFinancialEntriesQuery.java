package com.agenciahub.api.application.financial;

import com.agenciahub.api.domain.FinancialEntryCategory;
import com.agenciahub.api.domain.FinancialEntryStatus;
import com.agenciahub.api.domain.FinancialEntryType;

import java.time.LocalDate;
import java.util.UUID;

public record ListFinancialEntriesQuery(
        LocalDate from,
        LocalDate to,
        FinancialEntryType type,
        FinancialEntryCategory category,
        FinancialEntryStatus status,
        UUID customerId,
        String bankAccount) {
}
