package com.agenciahub.api.application.usecases.financial.shared;

import com.agenciahub.api.application.usecases.financial.shared.FinancialEntrySummaryResponseDTO;
import com.agenciahub.api.entity.Customer;
import com.agenciahub.api.entity.FinancialEntry;
import org.springframework.stereotype.Component;

@Component
public class FinancialEntryResponseMapper {

    public FinancialEntrySummaryResponseDTO toResponse(FinancialEntry e) {
        Customer c = e.getCustomer();
        return new FinancialEntrySummaryResponseDTO(
                e.getId(),
                e.getDescription(),
                e.getType(),
                e.getCategory(),
                e.getAmount(),
                e.getEntryDate(),
                e.getStatus(),
                c != null ? c.getId() : null,
                c != null ? c.getName() : null,
                e.getBankAccount());
    }
}
