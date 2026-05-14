package com.agenciahub.api.application.customer;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.customer.CustomerResponse;

import java.util.UUID;

public interface GetCustomerByIdUseCase extends UseCase<UUID, CustomerResponse> {
}
