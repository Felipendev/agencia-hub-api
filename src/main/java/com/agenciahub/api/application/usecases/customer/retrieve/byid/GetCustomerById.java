package com.agenciahub.api.application.usecases.customer.retrieve.byid;

import com.agenciahub.api.application.usecases.customer.shared.OutputMapper;
import com.agenciahub.api.application.usecases.customer.shared.CustomerSummaryResponseDTO;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetCustomerById implements GetCustomerByIdUseCase {

    private final CrmCustomerRepository customerRepository;

    @Override
    public CustomerSummaryResponseDTO execute(UUID id) {
        UUID agencyId = TenantContext.requireAgencyId();
        return customerRepository
                .findByIdAndAgency_Id(id, agencyId)
                .map(OutputMapper::toSummary)
                .orElseThrow(() -> new ResourceNotFoundException("cliente não encontrado: " + id));
    }
}
