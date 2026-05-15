package com.agenciahub.api.application.customer;

import com.agenciahub.api.dto.customer.CustomerResponse;
import com.agenciahub.api.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerResponseMapper {

    public CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(
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
