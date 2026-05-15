package com.agenciahub.api.application.usecases.customer.retrieve.list;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.customer.shared.CustomerSummaryResponseDTO;

import java.util.List;

public interface ListCustomersUseCase extends UseCase<ListCustomersQuery, List<CustomerSummaryResponseDTO>> {
}
