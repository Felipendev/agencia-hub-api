package com.agenciahub.api.application.usecases.customer.createcustomer;

import com.agenciahub.api.application.usecases.customer.CustomerPhoneNormalizer;
import com.agenciahub.api.application.usecases.customer.CustomerResponseMapper;
import com.agenciahub.api.dto.customer.CreateCustomerRequest;
import com.agenciahub.api.dto.customer.CustomerResponse;
import com.agenciahub.api.entity.Customer;
import com.agenciahub.api.exception.DuplicateCustomerException;
import com.agenciahub.api.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCustomer implements CreateCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final CustomerResponseMapper customerResponseMapper;

    @Override
    @Transactional
    public CustomerResponse execute(CreateCustomerRequest request) {
        String email = request.email().strip();
        String phone = CustomerPhoneNormalizer.normalize(request.phone());

        if (!email.isEmpty() && customerRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateCustomerException("e-mail", email);
        }
        if (!phone.isEmpty() && customerRepository.existsByNormalizedPhone(phone)) {
            throw new DuplicateCustomerException("telefone", request.phone().strip());
        }

        String notes = request.notes() != null ? request.notes() : "";
        Customer entity = Customer.builder()
                .name(request.name().strip())
                .email(email)
                .phone(request.phone().strip())
                .interestDestination(request.interestDestination().strip())
                .status(request.status())
                .notes(notes)
                .build();
        Customer saved = customerRepository.save(entity);
        return customerResponseMapper.toResponse(saved);
    }
}
