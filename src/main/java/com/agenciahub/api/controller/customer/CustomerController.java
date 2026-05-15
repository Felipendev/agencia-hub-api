package com.agenciahub.api.controller.customer;

import com.agenciahub.api.application.usecases.customer.createcustomer.CreateCustomerUseCase;
import com.agenciahub.api.application.usecases.customer.deletecustomer.DeleteCustomerUseCase;
import com.agenciahub.api.application.usecases.customer.getcustomerbyid.GetCustomerByIdUseCase;
import com.agenciahub.api.application.usecases.customer.listcustomers.ListCustomersQuery;
import com.agenciahub.api.application.usecases.customer.listcustomers.ListCustomersUseCase;
import com.agenciahub.api.application.usecases.customer.lookupcustomer.LookupCustomerQuery;
import com.agenciahub.api.application.usecases.customer.lookupcustomer.LookupCustomerUseCase;
import com.agenciahub.api.application.usecases.customer.updatecustomer.UpdateCustomerCommand;
import com.agenciahub.api.application.usecases.customer.updatecustomer.UpdateCustomerUseCase;
import com.agenciahub.api.application.controllers.docs.CustomerAPI;
import com.agenciahub.api.domain.CustomerStatus;
import com.agenciahub.api.dto.customer.CreateCustomerRequest;
import com.agenciahub.api.dto.customer.CustomerResponse;
import com.agenciahub.api.dto.customer.UpdateCustomerRequest;
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
    public List<CustomerResponse> list(String name, CustomerStatus status) {
        return listCustomersUseCase.execute(new ListCustomersQuery(name, status));
    }

    @Override
    public ResponseEntity<CustomerResponse> lookup(String email, String phone) {
        return lookupCustomerUseCase.execute(new LookupCustomerQuery(email, phone))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public CustomerResponse get(UUID id) {
        return getCustomerByIdUseCase.execute(id);
    }

    @Override
    public CustomerResponse create(CreateCustomerRequest request) {
        return createCustomerUseCase.execute(request);
    }

    @Override
    public CustomerResponse patch(UUID id, UpdateCustomerRequest request) {
        return updateCustomerUseCase.execute(new UpdateCustomerCommand(id, request));
    }

    @Override
    public void delete(UUID id) {
        deleteCustomerUseCase.execute(id);
    }
}
