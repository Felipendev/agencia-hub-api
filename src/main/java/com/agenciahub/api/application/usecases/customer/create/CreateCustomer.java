package com.agenciahub.api.application.usecases.customer.create;

import com.agenciahub.api.application.usecases.customer.shared.CustomerPhoneNormalizer;
import com.agenciahub.api.application.usecases.customer.shared.CustomerResponseMapper;
import com.agenciahub.api.application.usecases.customer.create.CreateCustomerRequestDTO;
import com.agenciahub.api.application.usecases.customer.shared.CustomerSummaryResponseDTO;
import com.agenciahub.api.entity.CrmCustomer;
import com.agenciahub.api.exception.DuplicateCustomerException;
import com.agenciahub.api.repository.CrmCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCustomer implements CreateCustomerUseCase {

    private final CrmCustomerRepository customerRepository;
    private final CustomerResponseMapper customerResponseMapper;

    @Override
    @Transactional
    public CustomerSummaryResponseDTO execute(CreateCustomerRequestDTO request) {
        String email = request.email().strip();
        String phone = CustomerPhoneNormalizer.normalize(request.phone());

        if (!email.isEmpty() && customerRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateCustomerException("e-mail", email);
        }
        if (!phone.isEmpty() && customerRepository.existsByNormalizedPhone(phone)) {
            throw new DuplicateCustomerException("telefone", request.phone().strip());
        }

        String notes = request.notes() != null ? request.notes() : "";
        CrmCustomer entity = CrmCustomer.builder()
                .name(request.name().strip())
                .email(email)
                .phone(request.phone().strip())
                .interestDestination(request.interestDestination().strip())
                .status(request.status())
                .notes(notes)
                .build();
        CrmCustomer saved = customerRepository.save(entity);
        return customerResponseMapper.toResponse(saved);
    }
}
