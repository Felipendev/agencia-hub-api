package com.agenciahub.api.application.usecases.financial.shared;

import com.agenciahub.api.application.usecases.financial.shared.FinancialEntrySummaryResponseDTO;
import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.application.persistence.entity.FinancialEntry;
import org.springframework.stereotype.Component;

@Component
public class FinancialEntryResponseMapper {

    public FinancialEntrySummaryResponseDTO toResponse(FinancialEntry e) {
        CrmCustomer c = e.getCustomer();
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
