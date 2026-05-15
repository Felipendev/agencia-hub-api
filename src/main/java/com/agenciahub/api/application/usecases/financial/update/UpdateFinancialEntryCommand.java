package com.agenciahub.api.application.usecases.financial.update;

import com.agenciahub.api.application.usecases.financial.update.UpdateFinancialEntryRequestDTO;

import java.util.UUID;

public record UpdateFinancialEntryCommand(UUID id, UpdateFinancialEntryRequestDTO request) {
}
