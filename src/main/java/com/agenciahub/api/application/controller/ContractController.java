package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.services.contract.ContractService;
import com.agenciahub.api.application.services.contract.ContractService.FileContent;
import com.agenciahub.api.domain.contract.Contracts.*;
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

/** Contratos por pessoa: rascunho, emissão versionada, envio, assinatura com evidência e cancelamento. */
@RestController
@RequestMapping("/contracts")
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService service;

    @GetMapping
    public List<ContractSummary> list(@RequestParam(required = false) UUID customerId,
                                      @RequestParam(required = false) Product product,
                                      @RequestParam(required = false) Status status) {
        return service.list(customerId, product, status);
    }

    @GetMapping("/{id}")
    public ContractDetail get(@PathVariable UUID id) { return service.detail(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContractDetail create(@RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody DraftInput input) {
        return service.create(key, input);
    }

    @PatchMapping("/{id}")
    public ContractDetail update(@PathVariable UUID id, @Valid @RequestBody DraftInput input) { return service.update(id, input); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam int expectedVersion) { service.delete(id, expectedVersion); }

    @GetMapping("/{id}/preview")
    public Preview preview(@PathVariable UUID id) { return service.preview(id); }

    @PostMapping("/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentMeta generate(@PathVariable UUID id, @Valid @RequestBody VersionInput input) {
        return service.generate(id, input.expectedVersion());
    }

    @GetMapping("/{id}/documents/{version}")
    public DocumentView document(@PathVariable UUID id, @PathVariable int version) { return service.document(id, version); }

    @GetMapping("/{id}/documents/{version}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id, @PathVariable int version) {
        return file(service.documentPdf(id, version), false);
    }

    @PostMapping("/{id}/send")
    public ContractDetail send(@PathVariable UUID id, @Valid @RequestBody SendInput input) { return service.send(id, input); }

    @PostMapping("/{id}/revise")
    public ContractDetail revise(@PathVariable UUID id, @Valid @RequestBody ReasonInput input) { return service.revise(id, input); }

    @PostMapping(value = "/{id}/sign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ContractDetail sign(@PathVariable UUID id, @RequestParam int expectedVersion,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate signedOn,
                               @RequestParam(required = false) String note,
                               @RequestPart("file") MultipartFile file) {
        return service.sign(id, expectedVersion, signedOn, note, file);
    }

    @PostMapping("/{id}/cancel")
    public ContractDetail cancel(@PathVariable UUID id, @Valid @RequestBody ReasonInput input) { return service.cancel(id, input); }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentMeta addAttachment(@PathVariable UUID id, @RequestParam(required = false) String description,
                                        @RequestPart("file") MultipartFile file) {
        return service.addAttachment(id, description, file);
    }

    @GetMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> attachment(@PathVariable UUID id, @PathVariable UUID attachmentId) {
        return file(service.attachment(id, attachmentId), true);
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttachment(@PathVariable UUID id, @PathVariable UUID attachmentId) { service.deleteAttachment(id, attachmentId); }

    @PutMapping("/{id}/receivables/{receivableId}")
    @PreAuthorize("hasRole('AGENCY_OWNER')")
    public ContractDetail link(@PathVariable UUID id, @PathVariable UUID receivableId) { return service.linkReceivable(id, receivableId); }

    @DeleteMapping("/{id}/receivables/{receivableId}")
    @PreAuthorize("hasRole('AGENCY_OWNER')")
    public ContractDetail unlink(@PathVariable UUID id, @PathVariable UUID receivableId) { return service.unlinkReceivable(id, receivableId); }

    private static ResponseEntity<byte[]> file(FileContent content, boolean attachment) {
        ContentDisposition disposition = (attachment ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(content.filename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(content.bytes());
    }
}
