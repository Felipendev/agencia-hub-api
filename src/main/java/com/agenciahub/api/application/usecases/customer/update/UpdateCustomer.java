package com.agenciahub.api.application.usecases.customer.update;

import com.agenciahub.api.application.usecases.customer.shared.CustomerPhoneNormalizer;
import com.agenciahub.api.application.usecases.customer.shared.CustomerResponseMapper;
import com.agenciahub.api.application.usecases.customer.shared.CustomerSummaryResponseDTO;
import com.agenciahub.api.application.usecases.customer.update.UpdateCustomerRequestDTO;
import com.agenciahub.api.entity.CrmCustomer;
import com.agenciahub.api.exception.DuplicateCustomerException;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.CrmCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateCustomer implements UpdateCustomerUseCase {

    private final CrmCustomerRepository customerRepository;
    private final CustomerResponseMapper customerResponseMapper;

    @Override
    public CustomerSummaryResponseDTO execute(UpdateCustomerCommand command) {
        UUID id = command.id();
        UpdateCustomerRequestDTO request = command.request();
        CrmCustomer entity = customerRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("cliente não encontrado: " + id));

        if (request.email() != null) {
            String email = request.email().strip();
            if (!email.isEmpty()
                    && !email.equalsIgnoreCase(entity.getEmail())
                    && customerRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
                throw new DuplicateCustomerException("e-mail", email);
            }
            entity.setEmail(email);
        }
        if (request.phone() != null) {
            String phone = CustomerPhoneNormalizer.normalize(request.phone());
            String currentNorm = CustomerPhoneNormalizer.normalize(entity.getPhone());
            if (!phone.isEmpty()
                    && !phone.equals(currentNorm)
                    && customerRepository.existsByNormalizedPhoneAndIdNot(phone, id)) {
                throw new DuplicateCustomerException("telefone", request.phone().strip());
            }
            entity.setPhone(request.phone().strip());
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

        return customerResponseMapper.toResponse(customerRepository.save(entity));
    }
}
