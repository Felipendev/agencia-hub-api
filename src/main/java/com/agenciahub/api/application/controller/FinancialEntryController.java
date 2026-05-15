package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.financial.create.CreateFinancialEntryUseCase;
import com.agenciahub.api.application.usecases.financial.retrieve.byid.GetFinancialEntryByIdUseCase;
import com.agenciahub.api.application.usecases.financial.retrieve.list.ListFinancialEntriesQuery;
import com.agenciahub.api.application.usecases.financial.retrieve.list.ListFinancialEntriesUseCase;
import com.agenciahub.api.application.usecases.financial.update.UpdateFinancialEntryCommand;
import com.agenciahub.api.application.usecases.financial.update.UpdateFinancialEntryUseCase;
import com.agenciahub.api.application.controller.doc.FinancialEntryAPI;
import com.agenciahub.api.domain.FinancialEntryCategory;
import com.agenciahub.api.domain.FinancialEntryStatus;
import com.agenciahub.api.domain.FinancialEntryType;
import com.agenciahub.api.application.usecases.financial.create.CreateFinancialEntryRequestDTO;
import com.agenciahub.api.application.usecases.financial.shared.FinancialEntrySummaryResponseDTO;
import com.agenciahub.api.application.usecases.financial.update.UpdateFinancialEntryRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGENCY_OWNER')")
public class FinancialEntryController implements FinancialEntryAPI {

    private final ListFinancialEntriesUseCase listFinancialEntriesUseCase;
    private final GetFinancialEntryByIdUseCase getFinancialEntryByIdUseCase;
    private final CreateFinancialEntryUseCase createFinancialEntryUseCase;
    private final UpdateFinancialEntryUseCase updateFinancialEntryUseCase;

    @Override
    public List<FinancialEntrySummaryResponseDTO> list(
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
    public FinancialEntrySummaryResponseDTO get(UUID id) {
        return getFinancialEntryByIdUseCase.execute(id);
    }

    @Override
    public FinancialEntrySummaryResponseDTO create(CreateFinancialEntryRequestDTO request) {
        return createFinancialEntryUseCase.execute(request);
    }

    @Override
    public FinancialEntrySummaryResponseDTO patch(UUID id, UpdateFinancialEntryRequestDTO request) {
        return updateFinancialEntryUseCase.execute(new UpdateFinancialEntryCommand(id, request));
    }
}
