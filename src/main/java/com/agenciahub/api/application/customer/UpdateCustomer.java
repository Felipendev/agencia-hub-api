package com.agenciahub.api.application.customer;

import com.agenciahub.api.dto.customer.CustomerResponse;
import com.agenciahub.api.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateCustomer implements UpdateCustomerUseCase {

    private final CustomerService customerService;

    @Override
    public CustomerResponse execute(UpdateCustomerCommand command) {
        return customerService.update(command.id(), command.request());
    }
}
