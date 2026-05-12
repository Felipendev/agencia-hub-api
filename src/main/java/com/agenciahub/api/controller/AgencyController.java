package com.agenciahub.api.controller;

import com.agenciahub.api.dto.agency.AgencyResponse;
import com.agenciahub.api.dto.agency.UpdateAgencyRequest;
import com.agenciahub.api.entity.Agency;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.UserRepository;
import com.agenciahub.api.service.AgencyService;
import com.agenciahub.api.validation.PhoneValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agency")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@Tag(name = "Agency")
public class AgencyController {

    private final AgencyService agencyService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get current user's agency data")
    public AgencyResponse getAgency() {
        Agency agency = agencyService.getCurrentAgency();
        return toResponse(agency);
    }

    @PatchMapping
    @Operation(summary = "Update agency fields")
    public AgencyResponse updateAgency(@Valid @RequestBody UpdateAgencyRequest request) {
        // Validate phone format if provided
        if (request.phone() != null && !request.phone().isBlank()) {
            if (!PhoneValidator.isValid(request.phone())) {
                throw new IllegalArgumentException("Formato de telefone inválido. Use DDD + número");
            }
        }

        User currentUser = getCurrentUser();
        Agency agency = agencyService.update(currentUser.getAgency().getId(), request, currentUser);
        return toResponse(agency);
    }

    private AgencyResponse toResponse(Agency agency) {
        return new AgencyResponse(
                agency.getId(),
                agency.getName(),
                agency.getPhone(),
                agency.getLogoUrl(),
                agency.getCnpj(),
                agency.getAddress(),
                agency.getAddressDetails(),
                agency.getCommercialEmail(),
                agency.getStatus(),
                agency.getSubscriptionStatus(),
                agency.getTrialEndsAt()
        );
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof User user) {
            return user;
        }
        throw new ResourceNotFoundException("Usuário não encontrado");
    }
}
