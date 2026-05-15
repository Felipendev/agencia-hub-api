package com.agenciahub.api.application.usecases.customer.lookupcustomer;

import com.agenciahub.api.application.usecases.customer.CustomerPhoneNormalizer;
import com.agenciahub.api.application.usecases.customer.CustomerResponseMapper;
import com.agenciahub.api.dto.customer.CustomerResponse;
import com.agenciahub.api.entity.Customer;
import com.agenciahub.api.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LookupCustomer implements LookupCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final CustomerResponseMapper customerResponseMapper;

    @Override
    public Optional<CustomerResponse> execute(LookupCustomerQuery query) {
        String email = query.email();
        if (email != null && !email.isBlank()) {
            Optional<Customer> byEmail = customerRepository.findFirstByEmailIgnoreCase(email.strip());
            if (byEmail.isPresent()) {
                return Optional.of(customerResponseMapper.toResponse(byEmail.get()));
            }
        }
        String phone = query.phone();
        if (phone != null && !phone.isBlank()) {
            String norm = CustomerPhoneNormalizer.normalize(phone);
            if (!norm.isEmpty()) {
                Optional<Customer> byPhone = customerRepository.findFirstByNormalizedPhone(norm);
                if (byPhone.isPresent()) {
                    return Optional.of(customerResponseMapper.toResponse(byPhone.get()));
                }
            }
        }
        return Optional.empty();
    }
}
