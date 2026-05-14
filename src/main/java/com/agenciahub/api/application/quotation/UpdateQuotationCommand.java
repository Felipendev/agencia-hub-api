package com.agenciahub.api.application.quotation;

import com.agenciahub.api.dto.quotation.UpdateQuotationRequest;

import java.util.UUID;

public record UpdateQuotationCommand(UUID id, UpdateQuotationRequest request) {
}
