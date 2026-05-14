package com.agenciahub.api.application.financial;

import com.agenciahub.api.dto.financial.UpdateFinancialEntryRequest;

import java.util.UUID;

public record UpdateFinancialEntryCommand(UUID id, UpdateFinancialEntryRequest request) {
}
