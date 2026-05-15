package com.agenciahub.api.application.usecases.customer.shared;

import com.agenciahub.api.entity.CrmCustomer;
import org.springframework.stereotype.Component;

@Component
public class CustomerResponseMapper {

    public CustomerSummaryResponseDTO toResponse(CrmCustomer c) {
        return new CustomerSummaryResponseDTO(
                c.getId(),
                c.getName(),
                c.getEmail(),
                c.getPhone(),
                c.getInterestDestination(),
                c.getStatus(),
                c.getNotes(),
                c.getCreatedAt());
    }
}
