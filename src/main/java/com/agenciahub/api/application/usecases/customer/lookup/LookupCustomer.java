package com.agenciahub.api.application.usecases.customer.lookup;

import com.agenciahub.api.application.usecases.customer.shared.CustomerPhoneNormalizer;
import com.agenciahub.api.application.usecases.customer.shared.OutputMapper;
import com.agenciahub.api.application.usecases.customer.shared.CustomerSummaryResponseDTO;
import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LookupCustomer implements LookupCustomerUseCase {

    private final CrmCustomerRepository customerRepository;

    @Override
    public Optional<CustomerSummaryResponseDTO> execute(LookupCustomerQuery query) {
        UUID agencyId = TenantContext.requireAgencyId();
        String email = query.email();
        if (email != null && !email.isBlank()) {
            Optional<CrmCustomer> byEmail = customerRepository
                    .findFirstByEmailIgnoreCaseAndAgency_Id(email.strip(), agencyId);
            if (byEmail.isPresent()) {
                return Optional.of(OutputMapper.toSummary(byEmail.get()));
            }
        }
        String phone = query.phone();
        if (phone != null && !phone.isBlank()) {
            String norm = CustomerPhoneNormalizer.normalize(phone);
            if (!norm.isEmpty()) {
                Optional<CrmCustomer> byPhone = customerRepository
                        .findFirstByNormalizedPhoneAndAgency_Id(norm, agencyId);
                if (byPhone.isPresent()) {
                    return Optional.of(OutputMapper.toSummary(byPhone.get()));
                }
            }
        }
        return Optional.empty();
    }
}
