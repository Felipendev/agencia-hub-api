package com.agenciahub.api.application.usecases.customer.lookup;

import com.agenciahub.api.application.usecases.customer.shared.CustomerPhoneNormalizer;
import com.agenciahub.api.application.usecases.customer.shared.CustomerResponseMapper;
import com.agenciahub.api.application.usecases.customer.shared.CustomerSummaryResponseDTO;
import com.agenciahub.api.entity.CrmCustomer;
import com.agenciahub.api.repository.CrmCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LookupCustomer implements LookupCustomerUseCase {

    private final CrmCustomerRepository customerRepository;
    private final CustomerResponseMapper customerResponseMapper;

    @Override
    public Optional<CustomerSummaryResponseDTO> execute(LookupCustomerQuery query) {
        String email = query.email();
        if (email != null && !email.isBlank()) {
            Optional<CrmCustomer> byEmail = customerRepository.findFirstByEmailIgnoreCase(email.strip());
            if (byEmail.isPresent()) {
                return Optional.of(customerResponseMapper.toResponse(byEmail.get()));
            }
        }
        String phone = query.phone();
        if (phone != null && !phone.isBlank()) {
            String norm = CustomerPhoneNormalizer.normalize(phone);
            if (!norm.isEmpty()) {
                Optional<CrmCustomer> byPhone = customerRepository.findFirstByNormalizedPhone(norm);
                if (byPhone.isPresent()) {
                    return Optional.of(customerResponseMapper.toResponse(byPhone.get()));
                }
            }
        }
        return Optional.empty();
    }
}
