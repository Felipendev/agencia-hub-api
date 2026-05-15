package com.agenciahub.api.application.usecases.customer.retrieve.byid;

import com.agenciahub.api.application.usecases.customer.shared.CustomerResponseMapper;
import com.agenciahub.api.application.usecases.customer.shared.CustomerSummaryResponseDTO;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.CrmCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetCustomerById implements GetCustomerByIdUseCase {

    private final CrmCustomerRepository customerRepository;
    private final CustomerResponseMapper customerResponseMapper;

    @Override
    public CustomerSummaryResponseDTO execute(UUID id) {
        return customerRepository
                .findById(id)
                .map(customerResponseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("cliente não encontrado: " + id));
    }
}
