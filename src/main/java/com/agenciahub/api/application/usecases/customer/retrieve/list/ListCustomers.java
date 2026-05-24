package com.agenciahub.api.application.usecases.customer.retrieve.list;

import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.usecases.customer.shared.CustomerSummaryResponseDTO;
import com.agenciahub.api.application.usecases.customer.shared.OutputMapper;
import com.agenciahub.api.domain.CustomerStatus;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListCustomers implements ListCustomersUseCase {

    private final CrmCustomerRepository customerRepository;

    @Override
    public List<CustomerSummaryResponseDTO> execute(ListCustomersQuery query) {
        UUID agencyId = TenantContext.requireAgencyId();
        String name = query.name();
        CustomerStatus status = query.status();
        boolean hasName = name != null && !name.isBlank();
        List<CrmCustomer> rows;
        if (!hasName && status == null) {
            rows = customerRepository.findAllByAgency_IdOrderByCreatedAtDesc(agencyId);
        } else if (hasName && status == null) {
            rows = customerRepository.findByAgency_IdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(agencyId, name.strip());
        } else if (!hasName) {
            rows = customerRepository.findByAgency_IdAndStatusOrderByCreatedAtDesc(agencyId, status);
        } else {
            rows = customerRepository.findByAgency_IdAndNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(agencyId, name.strip(), status);
        }
        return rows.stream().map(OutputMapper::toSummary).toList();
    }
}
