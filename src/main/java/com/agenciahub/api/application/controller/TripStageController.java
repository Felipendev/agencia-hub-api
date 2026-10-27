package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.services.stages.TripStageService;
import com.agenciahub.api.application.services.stages.TripStageService.Itinerary;
import com.agenciahub.api.application.services.stages.TripStageService.StageDetail;
import com.agenciahub.api.domain.stages.TripStages.StageInput;
import com.agenciahub.api.domain.stages.TripStages.StatusInput;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Etapas da viagem (hotel, transfer, atividades): mesma permissão de quem acessa viagens. */
@RestController
@RequestMapping("/trips/{tripId}/stages")
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
@RequiredArgsConstructor
public class TripStageController {

    private final TripStageService service;

    @GetMapping
    public Itinerary itinerary(@PathVariable UUID tripId) { return service.itinerary(tripId); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StageDetail create(@PathVariable UUID tripId, @RequestHeader("Idempotency-Key") UUID key, @RequestBody StageInput input) {
        return service.create(tripId, key, input);
    }

    @GetMapping("/{id}")
    public StageDetail get(@PathVariable UUID tripId, @PathVariable UUID id) { return service.detail(tripId, id); }

    @PatchMapping("/{id}")
    public StageDetail update(@PathVariable UUID tripId, @PathVariable UUID id, @RequestBody StageInput input) { return service.update(tripId, id, input); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID tripId, @PathVariable UUID id, @RequestParam int expectedVersion) { service.delete(tripId, id, expectedVersion); }

    @PostMapping("/{id}/status")
    public StageDetail status(@PathVariable UUID tripId, @PathVariable UUID id, @RequestBody StatusInput input) { return service.changeStatus(tripId, id, input); }

    @PutMapping("/{id}/attachments/{attachmentId}")
    public StageDetail linkAttachment(@PathVariable UUID tripId, @PathVariable UUID id, @PathVariable UUID attachmentId) {
        return service.linkAttachment(tripId, id, attachmentId);
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public StageDetail unlinkAttachment(@PathVariable UUID tripId, @PathVariable UUID id, @PathVariable UUID attachmentId) {
        return service.unlinkAttachment(tripId, id, attachmentId);
    }

    @PutMapping("/{id}/library/{libraryId}")
    public StageDetail linkLibrary(@PathVariable UUID tripId, @PathVariable UUID id, @PathVariable UUID libraryId) {
        return service.linkLibrary(tripId, id, libraryId);
    }

    @DeleteMapping("/{id}/library/{libraryId}")
    public StageDetail unlinkLibrary(@PathVariable UUID tripId, @PathVariable UUID id, @PathVariable UUID libraryId) {
        return service.unlinkLibrary(tripId, id, libraryId);
    }
}
