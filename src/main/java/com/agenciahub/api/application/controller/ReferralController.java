package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.services.referrals.ReferralService;
import com.agenciahub.api.application.services.referrals.ReferralService.*;
import com.agenciahub.api.domain.referrals.Referrals.Product;
import com.agenciahub.api.domain.referrals.Referrals.ReferralInput;
import com.agenciahub.api.domain.referrals.Referrals.Status;
import com.agenciahub.api.domain.referrals.Referrals.StatusInput;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Indicações indicador → indicado, por produto. Dono e vendedor, como o CRM. Sem créditos. */
@RestController
@RequestMapping("/referrals")
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService service;

    public record LinkRequest(Integer expectedVersion) {}

    @GetMapping
    public List<ReferralView> list(@RequestParam(required = false) Product product, @RequestParam(required = false) Status status,
                                   @RequestParam(required = false) UUID customerId,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                   @RequestParam(required = false) String q) {
        return service.list(new Filters(product, status, customerId, from, to, q));
    }

    @GetMapping("/summary")
    public Summary summary(@RequestParam(required = false) Product product,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.summary(product, from, to);
    }

    @GetMapping("/members")
    public List<Member> members() { return service.members(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Detail create(@RequestHeader("Idempotency-Key") UUID key, @RequestBody ReferralInput input) { return service.create(key, input); }

    @GetMapping("/{id}")
    public Detail get(@PathVariable UUID id) { return service.detail(id); }

    @PatchMapping("/{id}")
    public Detail update(@PathVariable UUID id, @RequestBody ReferralInput input) { return service.update(id, input); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam int expectedVersion) { service.delete(id, expectedVersion); }

    @PostMapping("/{id}/status")
    public Detail status(@PathVariable UUID id, @RequestBody StatusInput input) { return service.changeStatus(id, input); }

    @PutMapping("/{id}/referred-customer/{customerId}")
    public Detail linkReferred(@PathVariable UUID id, @PathVariable UUID customerId, @RequestBody(required = false) LinkRequest body) {
        return service.linkReferred(id, customerId, body == null ? null : body.expectedVersion());
    }
}
