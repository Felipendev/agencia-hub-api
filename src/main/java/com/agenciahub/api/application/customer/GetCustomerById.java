package com.agenciahub.api.application.customer;

import com.agenciahub.api.dto.customer.CustomerResponse;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetCustomerById implements GetCustomerByIdUseCase {

    private final CustomerRepository customerRepository;
    private final CustomerResponseMapper customerResponseMapper;

    @Override
    public CustomerResponse execute(UUID id) {
        return customerRepository
                .findById(id)
                .map(customerResponseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("cliente não encontrado: " + id));
    }
}
