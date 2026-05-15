package com.agenciahub.api.application.usecases.quotation.update;

import com.agenciahub.api.application.usecases.quotation.update.UpdateQuotationRequestDTO;

import java.util.UUID;

public record UpdateQuotationCommand(UUID id, UpdateQuotationRequestDTO request) {
}
