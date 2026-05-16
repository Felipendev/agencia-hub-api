package com.agenciahub.api.application.usecases.customer.create;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.customer.create.CreateCustomerRequestDTO;
import com.agenciahub.api.application.usecases.customer.shared.CustomerSummaryResponseDTO;

public interface CreateCustomerUseCase extends UseCase<CreateCustomerRequestDTO, CustomerSummaryResponseDTO> {
}
