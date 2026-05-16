package com.agenciahub.api.application.usecases.customer.retrieve.byid;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.customer.shared.CustomerSummaryResponseDTO;

import java.util.UUID;

public interface GetCustomerByIdUseCase extends UseCase<UUID, CustomerSummaryResponseDTO> {
}
