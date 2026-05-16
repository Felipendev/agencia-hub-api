package com.agenciahub.api.application.usecases.quotation.create;

import com.agenciahub.api.application.usecases.quotation.create.CreateQuotationRequestDTO;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;

public record CreateQuotationCommand(CreateQuotationRequestDTO request, PlatformAccount caller) {
}
