package com.agenciahub.api.controller.customer.docs;

import com.agenciahub.api.api.docs.StandardErrorApiResponses;
import com.agenciahub.api.domain.CustomerStatus;
import com.agenciahub.api.dto.customer.CreateCustomerRequest;
import com.agenciahub.api.dto.customer.CustomerResponse;
import com.agenciahub.api.dto.customer.UpdateCustomerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.UUID;

@Tag(
        name = "Clientes",
        description = """
                Cadastro e manutenção de clientes da agência.

                A exclusão é **permanente** e remove também cotações e desvincula lançamentos financeiros associados.""")
@RequestMapping("/customers")
@StandardErrorApiResponses
public interface CustomerAPI {

    @GetMapping
    @Operation(
            summary = "Lista clientes com filtros opcionais",
            description = "Filtros por nome (contém, case-insensitive) e/ou status.")
    List<CustomerResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) CustomerStatus status);

    @GetMapping("/lookup")
    @Operation(
            summary = "Busca cliente ativo por e-mail ou telefone",
            description = "Fluxo típico de importação: informe e-mail **ou** telefone. Resposta 404 se não houver correspondência ativa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "nenhum cliente ativo com o contato informado")
    })
    ResponseEntity<CustomerResponse> lookup(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone);

    @GetMapping("/{id}")
    @Operation(summary = "Obtém cliente por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "cliente inexistente")
    })
    CustomerResponse get(@Parameter(description = "id do cliente") @PathVariable UUID id);

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria cliente")
    CustomerResponse create(@Valid @RequestBody CreateCustomerRequest request);

    @PatchMapping("/{id}")
    @Operation(summary = "Atualiza parcialmente o cliente")
    CustomerResponse patch(
            @Parameter(description = "id do cliente") @PathVariable UUID id,
            @RequestBody UpdateCustomerRequest request);

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Exclui cliente permanentemente",
            description = "Remove o cliente, suas cotações e desvincula lançamentos financeiros. Não há restauração.")
    void delete(@Parameter(description = "id do cliente") @PathVariable UUID id);
}
