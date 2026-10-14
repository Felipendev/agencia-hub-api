package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.services.MilesWalletService;
import com.agenciahub.api.domain.MilesWallet.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController @RequiredArgsConstructor
@RequestMapping("/customers/{customerId}/miles-wallet")
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
public class MilesWalletController {
    private final MilesWalletService service;
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public com.agenciahub.api.exception.ApiError invalidBody() {
        return new com.agenciahub.api.exception.ApiError("Revise os campos da carteira: pontos e dias devem ser números inteiros, datas devem ser válidas e o tipo do registro deve ser informado.","INVALID_WALLET_INPUT");
    }
    @GetMapping public View get(@PathVariable UUID customerId) {return service.view(customerId);}
    @PutMapping("/profile") public ProfileRow saveProfile(@PathVariable UUID customerId,@Valid @RequestBody ProfileInput input,Principal principal) {return service.saveProfile(customerId,input,principal.getName());}
    @DeleteMapping("/profile") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveProfile(@PathVariable UUID customerId,@RequestParam int expectedVersion,Principal principal) {service.archiveProfile(customerId,expectedVersion,principal.getName());}
    @PostMapping("/items") @ResponseStatus(HttpStatus.CREATED)
    public Item create(@PathVariable UUID customerId,@RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody ItemInput input,Principal principal) {return service.create(customerId,key,input,principal.getName());}
    @PatchMapping("/items/{id}") public Item update(@PathVariable UUID customerId,@PathVariable UUID id,@Valid @RequestBody ItemInput input,Principal principal) {return service.update(customerId,id,input,principal.getName());}
    @DeleteMapping("/items/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID customerId,@PathVariable UUID id,@RequestParam int expectedVersion,Principal principal) {service.remove(customerId,id,expectedVersion,principal.getName());}
    @PostMapping("/items/{id}/restore") public Item restore(@PathVariable UUID customerId,@PathVariable UUID id,@RequestParam int expectedVersion,Principal principal) {return service.restore(customerId,id,expectedVersion,principal.getName());}
    @PostMapping("/accounts/{id}/movements") @ResponseStatus(HttpStatus.CREATED)
    public Movement move(@PathVariable UUID customerId,@PathVariable UUID id,@Valid @RequestBody MovementInput input,Principal principal) {return service.addMovement(customerId,id,input,principal.getName());}
    @PostMapping("/movements/{id}/reverse") public Movement reverse(@PathVariable UUID customerId,@PathVariable UUID id,@Valid @RequestBody ReversalInput input,Principal principal) {return service.reverse(customerId,id,input.reason(),principal.getName());}
}
