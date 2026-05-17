package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.usecases.admin.AgencyAdminSummaryDTO;
import com.agenciahub.api.application.usecases.admin.ListAgenciesForAdmin;
import com.agenciahub.api.application.usecases.agency.delete.CancelAccountDeletion;
import com.agenciahub.api.application.usecases.agency.delete.CancelAccountDeletionRequestDTO;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final ListAgenciesForAdmin listAgenciesForAdmin;
    private final CancelAccountDeletion cancelAccountDeletion;
    private final AgencyRepository agencyRepository;

    @GetMapping("/agencies")
    public List<AgencyAdminSummaryDTO> listAgencies() {
        return listAgenciesForAdmin.execute();
    }

    @PostMapping("/agencies/{id}/cancel-deletion")
    public ResponseEntity<Map<String, String>> cancelDeletion(@PathVariable UUID id) {
        cancelAccountDeletion.execute(new CancelAccountDeletionRequestDTO(id));
        return ResponseEntity.ok(Map.of("message", "exclusão cancelada com sucesso"));
    }

    @PostMapping("/agencies/{id}/suspend")
    public ResponseEntity<Map<String, String>> suspendAgency(@PathVariable UUID id) {
        Agency agency = agencyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("agência não encontrada"));
        agency.setStatus(AgencyStatus.SUSPENDED);
        agencyRepository.save(agency);
        return ResponseEntity.ok(Map.of("message", "agência suspensa com sucesso"));
    }

    @PostMapping("/agencies/{id}/reactivate")
    public ResponseEntity<Map<String, String>> reactivateAgency(@PathVariable UUID id) {
        Agency agency = agencyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("agência não encontrada"));
        AgencyStatus newStatus = agency.getTrialEndsAt() != null
                && agency.getTrialEndsAt().isAfter(java.time.Instant.now())
                ? AgencyStatus.TRIAL : AgencyStatus.ACTIVE;
        agency.setStatus(newStatus);
        agencyRepository.save(agency);
        return ResponseEntity.ok(Map.of("message", "agência reativada com sucesso"));
    }
}
