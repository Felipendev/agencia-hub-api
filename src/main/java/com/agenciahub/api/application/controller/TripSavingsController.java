package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.services.savings.TripSavingsService;
import com.agenciahub.api.application.services.savings.TripSavingsService.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Economia realizada por viagem (produto Viagens): mesma permissão de quem acessa viagens. */
@RestController
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
@RequiredArgsConstructor
public class TripSavingsController {

    private final TripSavingsService service;

    @GetMapping("/savings")
    public List<SavingsRow> list(@RequestParam(required = false) UUID customerId,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                 @RequestParam(required = false) Boolean available) {
        return service.list(customerId, from, to, available);
    }

    @GetMapping("/savings/report")
    public Report report(@RequestParam(required = false) UUID customerId,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.report(customerId, from, to);
    }

    @GetMapping("/trips/{tripId}/savings")
    public SavingsView get(@PathVariable UUID tripId) { return service.view(tripId); }

    @PutMapping("/trips/{tripId}/savings")
    public SavingsView save(@PathVariable UUID tripId, @Valid @RequestBody SaveInput input) { return service.save(tripId, input); }

    @DeleteMapping("/trips/{tripId}/savings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID tripId, @RequestParam int expectedVersion) { service.delete(tripId, expectedVersion); }

    @GetMapping("/trips/{tripId}/savings/versions/{version}")
    public VersionView version(@PathVariable UUID tripId, @PathVariable int version) { return service.version(tripId, version); }

    @PostMapping(value = "/trips/{tripId}/savings/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenceMeta addEvidence(@PathVariable UUID tripId, @RequestParam(required = false) String description,
                                    @RequestPart("file") MultipartFile file) {
        return service.addEvidence(tripId, description, file);
    }

    @GetMapping("/trips/{tripId}/savings/evidence/{evidenceId}")
    public ResponseEntity<byte[]> evidence(@PathVariable UUID tripId, @PathVariable UUID evidenceId) {
        return file(service.evidence(tripId, evidenceId));
    }

    @DeleteMapping("/trips/{tripId}/savings/evidence/{evidenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvidence(@PathVariable UUID tripId, @PathVariable UUID evidenceId) { service.deleteEvidence(tripId, evidenceId); }

    static ResponseEntity<byte[]> file(FileContent content) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(content.filename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(content.bytes());
    }
}
