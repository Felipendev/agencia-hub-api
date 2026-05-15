package com.agenciahub.api.application.usecases.customer.delete;

import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.persistence.repository.FinancialEntryRepository;
import com.agenciahub.api.application.persistence.repository.QuotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteCustomer implements DeleteCustomerUseCase {

    private final CrmCustomerRepository customerRepository;
    private final QuotationRepository quotationRepository;
    private final FinancialEntryRepository financialEntryRepository;

    @Override
    @Transactional
    public void execute(UUID id) {
        CrmCustomer entity = customerRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("cliente não encontrado: " + id));
        UUID customerId = entity.getId();
        quotationRepository.deleteAll(quotationRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId));
        financialEntryRepository.unlinkCustomer(customerId);
        customerRepository.delete(entity);
    }
}
