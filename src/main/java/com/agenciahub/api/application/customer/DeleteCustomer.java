package com.agenciahub.api.application.customer;

import com.agenciahub.api.entity.Customer;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.CustomerRepository;
import com.agenciahub.api.repository.FinancialEntryRepository;
import com.agenciahub.api.repository.QuotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteCustomer implements DeleteCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final QuotationRepository quotationRepository;
    private final FinancialEntryRepository financialEntryRepository;

    @Override
    @Transactional
    public void execute(UUID id) {
        Customer entity = customerRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("cliente não encontrado: " + id));
        UUID customerId = entity.getId();
        quotationRepository.deleteAll(quotationRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId));
        financialEntryRepository.unlinkCustomer(customerId);
        customerRepository.delete(entity);
    }
}
