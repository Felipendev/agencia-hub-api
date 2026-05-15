package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.quotation.create.CreateQuotationCommand;
import com.agenciahub.api.application.usecases.quotation.create.CreateQuotationUseCase;
import com.agenciahub.api.application.usecases.quotation.retrieve.byid.GetQuotationByIdUseCase;
import com.agenciahub.api.application.usecases.quotation.retrieve.list.ListQuotationsQuery;
import com.agenciahub.api.application.usecases.quotation.retrieve.list.ListQuotationsUseCase;
import com.agenciahub.api.application.usecases.quotation.delete.DeleteQuotationUseCase;
import com.agenciahub.api.application.usecases.quotation.update.UpdateQuotationCommand;
import com.agenciahub.api.application.usecases.quotation.update.UpdateQuotationUseCase;
import com.agenciahub.api.application.controller.doc.QuotationAPI;
import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.application.usecases.quotation.create.CreateQuotationRequestDTO;
import com.agenciahub.api.application.usecases.quotation.shared.QuotationSummaryResponseDTO;
import com.agenciahub.api.application.usecases.quotation.update.UpdateQuotationRequestDTO;
import com.agenciahub.api.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class QuotationController implements QuotationAPI {

    private final ListQuotationsUseCase listQuotationsUseCase;
    private final GetQuotationByIdUseCase getQuotationByIdUseCase;
    private final CreateQuotationUseCase createQuotationUseCase;
    private final UpdateQuotationUseCase updateQuotationUseCase;
    private final DeleteQuotationUseCase deleteQuotationUseCase;

    @Override
    public List<QuotationSummaryResponseDTO> list(
            UUID customerId,
            QuotationStatus status,
            String search,
            @AuthenticationPrincipal User caller) {
        return listQuotationsUseCase.execute(new ListQuotationsQuery(customerId, status, search, caller));
    }

    @Override
    public QuotationSummaryResponseDTO get(UUID id) {
        return getQuotationByIdUseCase.execute(id);
    }

    @Override
    public QuotationSummaryResponseDTO create(
            CreateQuotationRequestDTO request, @AuthenticationPrincipal User caller) {
        return createQuotationUseCase.execute(new CreateQuotationCommand(request, caller));
    }

    @Override
    public QuotationSummaryResponseDTO patch(UUID id, UpdateQuotationRequestDTO request) {
        return updateQuotationUseCase.execute(new UpdateQuotationCommand(id, request));
    }

    @Override
    public void delete(UUID id) {
        deleteQuotationUseCase.execute(id);
    }
}
