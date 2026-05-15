package com.agenciahub.api.application.usecases.quotation.create;

import com.agenciahub.api.application.usecases.quotation.create.CreateQuotationRequestDTO;
import com.agenciahub.api.entity.User;

public record CreateQuotationCommand(CreateQuotationRequestDTO request, User caller) {
}
