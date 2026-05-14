package com.agenciahub.api.application.customer;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.customer.CustomerResponse;

import java.util.Optional;

public interface LookupCustomerUseCase extends UseCase<LookupCustomerQuery, Optional<CustomerResponse>> {
}
