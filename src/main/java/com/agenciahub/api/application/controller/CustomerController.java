package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.customer.create.CreateCustomerUseCase;
import com.agenciahub.api.application.usecases.customer.delete.DeleteCustomerUseCase;
import com.agenciahub.api.application.usecases.customer.retrieve.byid.GetCustomerByIdUseCase;
import com.agenciahub.api.application.usecases.customer.retrieve.list.ListCustomersQuery;
import com.agenciahub.api.application.usecases.customer.retrieve.list.ListCustomersUseCase;
import com.agenciahub.api.application.usecases.customer.lookup.LookupCustomerQuery;
import com.agenciahub.api.application.usecases.customer.lookup.LookupCustomerUseCase;
import com.agenciahub.api.application.usecases.customer.update.UpdateCustomerCommand;
import com.agenciahub.api.application.usecases.customer.update.UpdateCustomerUseCase;
import com.agenciahub.api.application.controller.doc.CustomerAPI;
import com.agenciahub.api.domain.CustomerStatus;
import com.agenciahub.api.application.usecases.customer.create.CreateCustomerRequestDTO;
import com.agenciahub.api.application.usecases.customer.shared.CustomerSummaryResponseDTO;
import com.agenciahub.api.application.usecases.customer.update.UpdateCustomerRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CustomerController implements CustomerAPI {

    private final ListCustomersUseCase listCustomersUseCase;
    private final LookupCustomerUseCase lookupCustomerUseCase;
    private final GetCustomerByIdUseCase getCustomerByIdUseCase;
    private final CreateCustomerUseCase createCustomerUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final DeleteCustomerUseCase deleteCustomerUseCase;

    @Override
    public List<CustomerSummaryResponseDTO> list(String name, CustomerStatus status) {
        return listCustomersUseCase.execute(new ListCustomersQuery(name, status));
    }

    @Override
    public ResponseEntity<CustomerSummaryResponseDTO> lookup(String email, String phone) {
        return lookupCustomerUseCase.execute(new LookupCustomerQuery(email, phone))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public CustomerSummaryResponseDTO get(UUID id) {
        return getCustomerByIdUseCase.execute(id);
    }

    @Override
    public CustomerSummaryResponseDTO create(CreateCustomerRequestDTO request) {
        return createCustomerUseCase.execute(request);
    }

    @Override
    public CustomerSummaryResponseDTO patch(UUID id, UpdateCustomerRequestDTO request) {
        return updateCustomerUseCase.execute(new UpdateCustomerCommand(id, request));
    }

    @Override
    public void delete(UUID id) {
        deleteCustomerUseCase.execute(id);
    }
}
