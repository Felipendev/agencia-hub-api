package com.agenciahub.api.application.customer;

import com.agenciahub.api.dto.customer.CustomerResponse;
import com.agenciahub.api.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetCustomerById implements GetCustomerByIdUseCase {

    private final CustomerService customerService;

    @Override
    public CustomerResponse execute(UUID id) {
        return customerService.getById(id);
    }
}
