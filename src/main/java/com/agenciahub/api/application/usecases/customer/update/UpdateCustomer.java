package com.agenciahub.api.application.usecases.customer.update;

import com.agenciahub.api.application.usecases.customer.shared.CustomerPhoneNormalizer;
import com.agenciahub.api.application.usecases.customer.shared.OutputMapper;
import com.agenciahub.api.application.usecases.customer.shared.CustomerSummaryResponseDTO;
import com.agenciahub.api.application.usecases.customer.update.UpdateCustomerRequestDTO;
import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.exception.DuplicateCustomerException;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateCustomer implements UpdateCustomerUseCase {

    private final CrmCustomerRepository customerRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CustomerSummaryResponseDTO execute(UpdateCustomerCommand command) {
        UUID id = command.id();
        UUID agencyId = TenantContext.requireAgencyId();
        UpdateCustomerRequestDTO request = command.request();
        CrmCustomer entity = customerRepository
                .findByIdAndAgency_Id(id, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("cliente não encontrado: " + id));

        if (request.email() != null) {
            String email = request.email().strip();
            if (!email.isEmpty()
                    && !email.equalsIgnoreCase(entity.getEmail())
                    && customerRepository.existsByEmailIgnoreCaseAndAgency_IdAndIdNot(email, agencyId, id)) {
                throw new DuplicateCustomerException("e-mail", email);
            }
            entity.setEmail(email.isEmpty() ? null : email);
        }
        if (request.phone() != null) {
            String phone = CustomerPhoneNormalizer.normalize(request.phone());
            String currentNorm = CustomerPhoneNormalizer.normalize(entity.getPhone());
            if (!phone.isEmpty()
                    && !phone.equals(currentNorm)
                    && customerRepository.existsByNormalizedPhoneAndAgency_IdAndIdNot(phone, agencyId, id)) {
                throw new DuplicateCustomerException("telefone", request.phone().strip());
            }
            entity.setPhone(phone.isEmpty() ? null : request.phone().strip());
        }
        if (request.name() != null) {
            entity.setName(request.name().strip());
        }
        if (request.interestDestination() != null) {
            entity.setInterestDestination(request.interestDestination().strip());
        }
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        if (request.notes() != null) {
            entity.setNotes(request.notes());
        }

        if (request.profileData() != null) {
            entity.setProfileData(com.agenciahub.api.application.usecases.customer.shared.CustomerProfile.merge(
                    entity.getProfileData(), request.profileData()));
        }
        return OutputMapper.toSummary(customerRepository.save(entity));
    }
}
