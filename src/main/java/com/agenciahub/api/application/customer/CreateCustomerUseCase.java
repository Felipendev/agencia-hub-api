package com.agenciahub.api.application.customer;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.customer.CreateCustomerRequest;
import com.agenciahub.api.dto.customer.CustomerResponse;

public interface CreateCustomerUseCase extends UseCase<CreateCustomerRequest, CustomerResponse> {
}
