package com.agenciahub.api.application.usecases.customer.listcustomers;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.customer.CustomerResponse;

import java.util.List;

public interface ListCustomersUseCase extends UseCase<ListCustomersQuery, List<CustomerResponse>> {
}
