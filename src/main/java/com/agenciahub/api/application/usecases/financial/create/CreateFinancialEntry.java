package com.agenciahub.api.application.usecases.financial.create;

import com.agenciahub.api.application.usecases.financial.shared.FinancialEntryResponseMapper;
import com.agenciahub.api.application.usecases.financial.create.CreateFinancialEntryRequestDTO;
import com.agenciahub.api.application.usecases.financial.shared.FinancialEntrySummaryResponseDTO;
import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.application.persistence.entity.FinancialEntry;
import com.agenciahub.api.application.persistence.entity.Supplier;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.FinancialEntryRepository;
import com.agenciahub.api.application.persistence.repository.SupplierRepository;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateFinancialEntry implements CreateFinancialEntryUseCase {

    private final FinancialEntryRepository financialEntryRepository;
    private final CrmCustomerRepository customerRepository;
    private final AgencyRepository agencyRepository;
    private final SupplierRepository supplierRepository;
    private final FinancialEntryResponseMapper financialEntryResponseMapper;

    @Override
    public FinancialEntrySummaryResponseDTO execute(CreateFinancialEntryRequestDTO request) {
        UUID agencyId = TenantContext.requireAgencyId();
        CrmCustomer customer = resolveCustomer(request.customerId(), agencyId);
        Supplier supplier = resolveSupplier(request.supplierId(), agencyId);
        Agency agency = agencyRepository.getReferenceById(agencyId);
        FinancialEntry entity = FinancialEntry.builder()
                .agency(agency)
                .description(request.description().strip())
                .type(request.type())
                .category(request.category())
                .amount(request.amount())
                .entryDate(request.entryDate())
                .status(request.status())
                .customer(customer)
                .supplier(supplier)
                .bankAccount(blankToNull(request.bankAccount()))
                .recurrenceFrequency(blankToNull(request.recurrenceFrequency()))
                .notes(blankToNull(request.notes()))
                .saleAmount(request.saleAmount())
                .supplierCost(request.supplierCost())
                .commissionAmount(request.commissionAmount())
                .build();
        FinancialEntry saved = financialEntryRepository.save(entity);
        return financialEntryResponseMapper.toResponse(saved);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.strip();
        return t.isEmpty() ? null : t;
    }

    private CrmCustomer resolveCustomer(UUID customerId, UUID agencyId) {
        if (customerId == null) {
            return null;
        }
        return customerRepository.findByIdAndAgency_Id(customerId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("cliente não encontrado: " + customerId));
    }

    private Supplier resolveSupplier(UUID supplierId, UUID agencyId) {
        if (supplierId == null) return null;
        return supplierRepository.findByIdAndAgency_Id(supplierId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("fornecedor não encontrado: " + supplierId));
    }
}
