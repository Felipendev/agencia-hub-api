package com.agenciahub.api.application.usecases.customer.create;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.usecases.customer.shared.CustomerSummaryResponseDTO;
import com.agenciahub.api.application.usecases.customer.shared.OutputMapper;
import com.agenciahub.api.exception.DuplicateCustomerException;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCustomer implements CreateCustomerUseCase {

    private final CrmCustomerRepository customerRepository;
    private final AgencyRepository agencyRepository;

    @Override
    @Transactional
    public CustomerSummaryResponseDTO execute(CreateCustomerRequestDTO request) {
        String email = request.email().strip();
        String phone = InputMapper.normalizedPhone(request);

        if (!email.isEmpty() && customerRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateCustomerException("e-mail", email);
        }

        Agency agency = agencyRepository.getReferenceById(TenantContext.requireAgencyId());
        CrmCustomer entity = InputMapper.toNewEntity(request);
        entity.setAgency(agency);
        CrmCustomer saved = customerRepository.save(entity);
        return OutputMapper.toSummary(saved);
    }
}
