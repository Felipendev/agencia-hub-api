package com.agenciahub.api.application.usecases.customer.retrieve.list;

import com.agenciahub.api.application.usecases.customer.shared.CustomerResponseMapper;
import com.agenciahub.api.domain.CustomerStatus;
import com.agenciahub.api.application.usecases.customer.shared.CustomerSummaryResponseDTO;
import com.agenciahub.api.entity.CrmCustomer;
import com.agenciahub.api.repository.CrmCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListCustomers implements ListCustomersUseCase {

    private final CrmCustomerRepository customerRepository;
    private final CustomerResponseMapper customerResponseMapper;

    @Override
    public List<CustomerSummaryResponseDTO> execute(ListCustomersQuery query) {
        String name = query.name();
        CustomerStatus status = query.status();
        boolean hasName = name != null && !name.isBlank();
        List<CrmCustomer> rows;
        if (!hasName && status == null) {
            rows = customerRepository.findAllByOrderByCreatedAtDesc();
        } else if (hasName && status == null) {
            rows = customerRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc(name.strip());
        } else if (!hasName) {
            rows = customerRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            rows = customerRepository.findByNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(name.strip(), status);
        }
        return rows.stream().map(customerResponseMapper::toResponse).toList();
    }
}
