package com.agenciahub.api.service;

import com.agenciahub.api.domain.CustomerStatus;
import com.agenciahub.api.dto.customer.CreateCustomerRequest;
import com.agenciahub.api.dto.customer.CustomerResponse;
import com.agenciahub.api.dto.customer.UpdateCustomerRequest;
import com.agenciahub.api.entity.Customer;
import com.agenciahub.api.exception.DuplicateCustomerException;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.CustomerRepository;
import com.agenciahub.api.repository.FinancialEntryRepository;
import com.agenciahub.api.repository.OpportunityRepository;
import com.agenciahub.api.repository.QuotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final QuotationRepository quotationRepository;
    private final OpportunityRepository opportunityRepository;
    private final FinancialEntryRepository financialEntryRepository;

    @Transactional(readOnly = true)
    public List<CustomerResponse> search(String name, CustomerStatus status) {
        boolean hasName = name != null && !name.isBlank();
        List<Customer> rows;
        if (!hasName && status == null) {
            rows = customerRepository.findByDeletedAtIsNullOrderByCreatedAtDesc();
        } else if (hasName && status == null) {
            rows = customerRepository.findByDeletedAtIsNullAndNameContainingIgnoreCaseOrderByCreatedAtDesc(name.strip());
        } else if (!hasName) {
            rows = customerRepository.findByDeletedAtIsNullAndStatusOrderByCreatedAtDesc(status);
        } else {
            rows = customerRepository.findByDeletedAtIsNullAndNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
                    name.strip(), status);
        }
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(UUID id) {
        return customerRepository.findByIdAndDeletedAtIsNull(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<CustomerResponse> lookupActiveByContact(String email, String phone) {
        if (email != null && !email.isBlank()) {
            Optional<Customer> byEmail =
                    customerRepository.findFirstByDeletedAtIsNullAndEmailIgnoreCase(email.strip());
            if (byEmail.isPresent()) {
                return Optional.of(toResponse(byEmail.get()));
            }
        }
        if (phone != null && !phone.isBlank()) {
            String norm = normalizePhone(phone);
            if (!norm.isEmpty()) {
                Optional<Customer> byPhone = customerRepository.findFirstActiveByNormalizedPhone(norm);
                if (byPhone.isPresent()) {
                    return Optional.of(toResponse(byPhone.get()));
                }
            }
        }
        return Optional.empty();
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        String email = request.email().strip();
        String phone = normalizePhone(request.phone());

        if (!email.isEmpty() && customerRepository.existsByDeletedAtIsNullAndEmailIgnoreCase(email)) {
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
        return toResponse(saved);
    }

    @Transactional
    public CustomerResponse update(UUID id, UpdateCustomerRequest request) {
        Customer entity = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));

        if (request.email() != null) {
            String email = request.email().strip();
            if (!email.isEmpty()
                    && !email.equalsIgnoreCase(entity.getEmail())
                    && customerRepository.existsByDeletedAtIsNullAndEmailIgnoreCaseAndIdNot(email, id)) {
                throw new DuplicateCustomerException("e-mail", email);
            }
            entity.setEmail(email);
        }
        if (request.phone() != null) {
            String phone = normalizePhone(request.phone());
            String currentNorm = normalizePhone(entity.getPhone());
            if (!phone.isEmpty()
                    && !phone.equals(currentNorm)
                    && customerRepository.existsByNormalizedPhoneAndIdNot(phone, id)) {
                throw new DuplicateCustomerException("telefone", request.phone().strip());
            }
            entity.setPhone(request.phone().strip());
        }
        if (request.name() != null)                entity.setName(request.name().strip());
        if (request.interestDestination() != null)  entity.setInterestDestination(request.interestDestination().strip());
        if (request.status() != null)               entity.setStatus(request.status());
        if (request.notes() != null)                entity.setNotes(request.notes());

        return toResponse(entity);
    }

    @Transactional
    public void delete(UUID id) {
        Customer entity = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + id));
        UUID customerId = entity.getId();
        opportunityRepository.deleteByCustomer_Id(customerId);
        quotationRepository.deleteAll(quotationRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId));
        financialEntryRepository.unlinkCustomer(customerId);
        customerRepository.delete(entity);
    }

    @Transactional
    public CustomerResponse restore(UUID id) {
        Customer entity = customerRepository.findByIdAndDeletedAtIsNotNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado na lixeira: " + id));
        entity.setDeletedAt(null);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> listDeleted() {
        return customerRepository.findByDeletedAtIsNotNullOrderByDeletedAtDesc().stream()
                .map(this::toResponse).toList();
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("\\D", "");
    }

    private CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(
                c.getId(),
                c.getName(),
                c.getEmail(),
                c.getPhone(),
                c.getInterestDestination(),
                c.getStatus(),
                c.getNotes(),
                c.getCreatedAt(),
                c.getDeletedAt()
        );
    }
}