package com.agenciahub.api.application.customer;

import com.agenciahub.api.dto.customer.CustomerResponse;
import com.agenciahub.api.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LookupCustomer implements LookupCustomerUseCase {

    private final CustomerService customerService;

    @Override
    public Optional<CustomerResponse> execute(LookupCustomerQuery query) {
        return customerService.lookupActiveByContact(query.email(), query.phone());
    }
}
