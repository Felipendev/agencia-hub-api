package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.services.agenda.AgendaService;
import com.agenciahub.api.application.services.agenda.AgendaService.*;
import com.agenciahub.api.domain.agenda.Agenda.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Agenda e atas de atendimento (F07): dono e vendedor da agência. */
@RestController
@RequestMapping("/appointments")
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
@RequiredArgsConstructor
public class AgendaController {

    private final AgendaService service;

    @GetMapping
    public List<AppointmentView> list(@RequestParam(required = false) UUID customerId,
                                      @RequestParam(required = false) Product product,
                                      @RequestParam(required = false) Status status,
                                      @RequestParam(required = false) UUID responsibleId,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                      @RequestParam(required = false) String q) {
        return service.list(new Filters(customerId, product, status, responsibleId, from, to, q));
    }

    @GetMapping("/members")
    public List<Member> members() { return service.members(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Detail create(@RequestHeader("Idempotency-Key") UUID key, @RequestBody AppointmentInput input) {
        return service.create(key, input);
    }

    @GetMapping("/{id}")
    public Detail get(@PathVariable UUID id) { return service.detail(id); }

    @PatchMapping("/{id}")
    public Detail update(@PathVariable UUID id, @RequestBody AppointmentInput input) { return service.update(id, input); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam int expectedVersion) { service.delete(id, expectedVersion); }

    @PostMapping("/{id}/status")
    public Detail status(@PathVariable UUID id, @RequestBody StatusInput input) { return service.changeStatus(id, input); }

    @PutMapping("/{id}/minutes")
    public Detail minutes(@PathVariable UUID id, @RequestBody MinutesInput input) { return service.saveMinutes(id, input); }

    @GetMapping("/{id}/minutes/versions/{version}")
    public MinutesVersion minutesVersion(@PathVariable UUID id, @PathVariable int version) { return service.minutesVersion(id, version); }

    @GetMapping("/{id}/ics")
    public ResponseEntity<byte[]> ics(@PathVariable UUID id) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("compromisso-" + id + ".ics").build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(service.ics(id).getBytes(StandardCharsets.UTF_8));
    }
}
