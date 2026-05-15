package com.agenciahub.api.application.usecases.customer.lookup;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.customer.shared.CustomerSummaryResponseDTO;

import java.util.Optional;

public interface LookupCustomerUseCase extends UseCase<LookupCustomerQuery, Optional<CustomerSummaryResponseDTO>> {
}
