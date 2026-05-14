package com.agenciahub.api.application.customer;

import com.agenciahub.api.dto.customer.CreateCustomerRequest;
import com.agenciahub.api.dto.customer.CustomerResponse;
import com.agenciahub.api.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateCustomer implements CreateCustomerUseCase {

    private final CustomerService customerService;

    @Override
    public CustomerResponse execute(CreateCustomerRequest request) {
        return customerService.create(request);
    }
}
