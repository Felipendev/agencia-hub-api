package com.agenciahub.api.application.usecases.financial.shared;

import com.agenciahub.api.application.usecases.financial.shared.FinancialEntrySummaryResponseDTO;
import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.application.persistence.entity.FinancialEntry;
import com.agenciahub.api.application.persistence.entity.Supplier;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class FinancialEntryResponseMapper {

    public FinancialEntrySummaryResponseDTO toResponse(FinancialEntry e) {
        CrmCustomer c = e.getCustomer();
        Supplier s = e.getSupplier();
        BigDecimal profit = e.getSaleAmount() == null ? null
                : e.getSaleAmount().subtract(e.getSupplierCost() == null ? BigDecimal.ZERO : e.getSupplierCost())
                        .subtract(e.getCommissionAmount() == null ? BigDecimal.ZERO : e.getCommissionAmount());
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
                s != null ? s.getId() : null,
                s != null ? s.getName() : null,
                e.getBankAccount(),
                e.getRecurrenceFrequency(),
                e.getNotes(),
                e.getSaleAmount(),
                e.getSupplierCost(),
                e.getCommissionAmount(),
                profit);
    }
}
