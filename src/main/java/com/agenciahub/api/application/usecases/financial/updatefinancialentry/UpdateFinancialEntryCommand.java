package com.agenciahub.api.application.usecases.financial.updatefinancialentry;

import com.agenciahub.api.dto.financial.UpdateFinancialEntryRequest;

import java.util.UUID;

public record UpdateFinancialEntryCommand(UUID id, UpdateFinancialEntryRequest request) {
}
