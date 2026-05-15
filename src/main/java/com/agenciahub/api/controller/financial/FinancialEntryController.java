package com.agenciahub.api.controller.financial;

import com.agenciahub.api.application.usecases.financial.createfinancialentry.CreateFinancialEntryUseCase;
import com.agenciahub.api.application.usecases.financial.getfinancialentrybyid.GetFinancialEntryByIdUseCase;
import com.agenciahub.api.application.usecases.financial.listfinancialentries.ListFinancialEntriesQuery;
import com.agenciahub.api.application.usecases.financial.listfinancialentries.ListFinancialEntriesUseCase;
import com.agenciahub.api.application.usecases.financial.updatefinancialentry.UpdateFinancialEntryCommand;
import com.agenciahub.api.application.usecases.financial.updatefinancialentry.UpdateFinancialEntryUseCase;
import com.agenciahub.api.application.controllers.docs.FinancialEntryAPI;
import com.agenciahub.api.domain.FinancialEntryCategory;
import com.agenciahub.api.domain.FinancialEntryStatus;
import com.agenciahub.api.domain.FinancialEntryType;
import com.agenciahub.api.dto.financial.CreateFinancialEntryRequest;
import com.agenciahub.api.dto.financial.FinancialEntryResponse;
import com.agenciahub.api.dto.financial.UpdateFinancialEntryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class FinancialEntryController implements FinancialEntryAPI {

    private final ListFinancialEntriesUseCase listFinancialEntriesUseCase;
    private final GetFinancialEntryByIdUseCase getFinancialEntryByIdUseCase;
    private final CreateFinancialEntryUseCase createFinancialEntryUseCase;
    private final UpdateFinancialEntryUseCase updateFinancialEntryUseCase;

    @Override
    public List<FinancialEntryResponse> list(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            FinancialEntryType type,
            FinancialEntryCategory category,
            FinancialEntryStatus status,
            UUID customerId,
            String bankAccount) {
        return listFinancialEntriesUseCase.execute(
                new ListFinancialEntriesQuery(from, to, type, category, status, customerId, bankAccount));
    }

    @Override
    public FinancialEntryResponse get(UUID id) {
        return getFinancialEntryByIdUseCase.execute(id);
    }

    @Override
    public FinancialEntryResponse create(CreateFinancialEntryRequest request) {
        return createFinancialEntryUseCase.execute(request);
    }

    @Override
    public FinancialEntryResponse patch(UUID id, UpdateFinancialEntryRequest request) {
        return updateFinancialEntryUseCase.execute(new UpdateFinancialEntryCommand(id, request));
    }
}
