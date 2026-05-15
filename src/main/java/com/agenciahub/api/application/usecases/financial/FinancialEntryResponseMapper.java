package com.agenciahub.api.application.usecases.financial;

import com.agenciahub.api.dto.financial.FinancialEntryResponse;
import com.agenciahub.api.entity.Customer;
import com.agenciahub.api.entity.FinancialEntry;
import org.springframework.stereotype.Component;

@Component
public class FinancialEntryResponseMapper {

    public FinancialEntryResponse toResponse(FinancialEntry e) {
        Customer c = e.getCustomer();
        return new FinancialEntryResponse(
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
