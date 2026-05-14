package com.agenciahub.api.application.customer;

import com.agenciahub.api.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteCustomer implements DeleteCustomerUseCase {

    private final CustomerService customerService;

    @Override
    public void execute(UUID id) {
        customerService.delete(id);
    }
}
