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
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/agency")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@Tag(name = "Agency")
public class AgencyController {

    private static final long MAX_LOGO_SIZE = 2 * 1024 * 1024; // 2MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/svg+xml"
    );

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

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload agency logo (PNG, JPG, SVG, max 2MB)")
    public Map<String, String> uploadLogo(@RequestParam("file") MultipartFile file) {
        // Validate file size
        if (file.getSize() > MAX_LOGO_SIZE) {
            throw new IllegalArgumentException("Imagem deve ter no máximo 2MB");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Formatos aceitos: PNG, JPG, SVG");
        }

        // For now, just store a placeholder URL. Actual file storage can be added later.
        String logoUrl = "/uploads/logos/" + UUID.randomUUID() + getExtension(contentType);

        User currentUser = getCurrentUser();
        agencyService.updateLogo(currentUser.getAgency().getId(), logoUrl, currentUser);

        return Map.of("logoUrl", logoUrl, "message", "Logo atualizado com sucesso");
    }

    private String getExtension(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/svg+xml" -> ".svg";
            default -> "";
        };
    }

    private AgencyResponse toResponse(Agency agency) {
        return new AgencyResponse(
                agency.getId(),
                agency.getName(),
                agency.getPhone(),
                agency.getLogoUrl(),
                agency.getCnpj(),
                agency.getAddress(),
                agency.getCommercialEmail(),
                agency.getStatus(),
                agency.getSubscriptionStatus(),
                agency.getTrialEndsAt()
        );
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(authentication.getName());
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }
}
