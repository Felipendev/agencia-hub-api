package com.agenciahub.api.application.quotation;

import com.agenciahub.api.dto.quotation.CreateQuotationRequest;
import com.agenciahub.api.entity.User;

public record CreateQuotationCommand(CreateQuotationRequest request, User caller) {
}
