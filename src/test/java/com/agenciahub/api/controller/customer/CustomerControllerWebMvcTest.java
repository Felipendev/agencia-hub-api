package com.agenciahub.api.controller.customer;

import com.agenciahub.api.application.usecases.customer.createcustomer.CreateCustomerUseCase;
import com.agenciahub.api.application.usecases.customer.deletecustomer.DeleteCustomerUseCase;
import com.agenciahub.api.application.usecases.customer.getcustomerbyid.GetCustomerByIdUseCase;
import com.agenciahub.api.application.usecases.customer.listcustomers.ListCustomersQuery;
import com.agenciahub.api.application.usecases.customer.listcustomers.ListCustomersUseCase;
import com.agenciahub.api.application.usecases.customer.lookupcustomer.LookupCustomerUseCase;
import com.agenciahub.api.application.usecases.customer.updatecustomer.UpdateCustomerUseCase;
import com.agenciahub.api.domain.CustomerStatus;
import com.agenciahub.api.dto.customer.CreateCustomerRequest;
import com.agenciahub.api.dto.customer.CustomerResponse;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.security.JwtAuthFilter;
import com.agenciahub.api.security.RateLimitFilter;
import com.agenciahub.api.support.WebMvcControllerTestImports;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CustomerController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcControllerTestImports.class)
class CustomerControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private ListCustomersUseCase listCustomersUseCase;

    @MockitoBean
    private LookupCustomerUseCase lookupCustomerUseCase;

    @MockitoBean
    private GetCustomerByIdUseCase getCustomerByIdUseCase;

    @MockitoBean
    private CreateCustomerUseCase createCustomerUseCase;

    @MockitoBean
    private UpdateCustomerUseCase updateCustomerUseCase;

    @MockitoBean
    private DeleteCustomerUseCase deleteCustomerUseCase;

    @Test
    void list_returnsCustomers() throws Exception {
        UUID id = UUID.randomUUID();
        when(listCustomersUseCase.execute(any(ListCustomersQuery.class)))
                .thenReturn(List.of(new CustomerResponse(
                        id,
                        "Ana",
                        "ana@test.com",
                        "11999999999",
                        "Europa",
                        CustomerStatus.PROSPECT,
                        null,
                        Instant.parse("2026-01-01T00:00:00Z"))));

        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ana"));
    }

    @Test
    void get_whenMissing_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(getCustomerByIdUseCase.execute(eq(id)))
                .thenThrow(new ResourceNotFoundException("cliente não encontrado"));

        mockMvc.perform(get("/customers/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void create_happyPath_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(createCustomerUseCase.execute(any(CreateCustomerRequest.class)))
                .thenReturn(new CustomerResponse(
                        id,
                        "Ana",
                        "ana@test.com",
                        "11999999999",
                        "Europa",
                        CustomerStatus.PROSPECT,
                        null,
                        Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ana",
                                  "email": "ana@test.com",
                                  "phone": "11999999999",
                                  "interestDestination": "Europa",
                                  "status": "PROSPECT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void create_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ana",
                                  "email": "invalid",
                                  "phone": "11999999999",
                                  "interestDestination": "Europa",
                                  "status": "PROSPECT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
