package com.agenciahub.api.application.customer;

import com.agenciahub.api.dto.customer.CustomerResponse;
import com.agenciahub.api.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListCustomers implements ListCustomersUseCase {

    private final CustomerService customerService;

    @Override
    public List<CustomerResponse> execute(ListCustomersQuery query) {
        return customerService.search(query.name(), query.status());
    }
}
