package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.quotation.createquotation.CreateQuotationCommand;
import com.agenciahub.api.application.usecases.quotation.createquotation.CreateQuotationUseCase;
import com.agenciahub.api.application.usecases.quotation.getquotationbyid.GetQuotationByIdUseCase;
import com.agenciahub.api.application.usecases.quotation.listquotations.ListQuotationsQuery;
import com.agenciahub.api.application.usecases.quotation.listquotations.ListQuotationsUseCase;
import com.agenciahub.api.application.usecases.quotation.deletequotation.DeleteQuotationUseCase;
import com.agenciahub.api.application.usecases.quotation.updatequotation.UpdateQuotationCommand;
import com.agenciahub.api.application.usecases.quotation.updatequotation.UpdateQuotationUseCase;
import com.agenciahub.api.application.controller.doc.QuotationAPI;
import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.dto.quotation.CreateQuotationRequest;
import com.agenciahub.api.dto.quotation.QuotationResponse;
import com.agenciahub.api.dto.quotation.UpdateQuotationRequest;
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
    public List<QuotationResponse> list(
            UUID customerId,
            QuotationStatus status,
            String search,
            @AuthenticationPrincipal User caller) {
        return listQuotationsUseCase.execute(new ListQuotationsQuery(customerId, status, search, caller));
    }

    @Override
    public QuotationResponse get(UUID id) {
        return getQuotationByIdUseCase.execute(id);
    }

    @Override
    public QuotationResponse create(
            CreateQuotationRequest request, @AuthenticationPrincipal User caller) {
        return createQuotationUseCase.execute(new CreateQuotationCommand(request, caller));
    }

    @Override
    public QuotationResponse patch(UUID id, UpdateQuotationRequest request) {
        return updateQuotationUseCase.execute(new UpdateQuotationCommand(id, request));
    }

    @Override
    public void delete(UUID id) {
        deleteQuotationUseCase.execute(id);
    }
}
