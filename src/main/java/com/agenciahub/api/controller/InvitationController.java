package com.agenciahub.api.controller;

import com.agenciahub.api.dto.invitation.CreateInvitationRequest;
import com.agenciahub.api.dto.invitation.InvitationResponse;
import com.agenciahub.api.entity.Invitation;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.UserRepository;
import com.agenciahub.api.security.TenantContext;
import com.agenciahub.api.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@Tag(name = "Invitations")
public class InvitationController {

    private final InvitationService invitationService;
    private final UserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new invitation (OWNER only)")
    public InvitationResponse create(@Valid @RequestBody CreateInvitationRequest request) {
        User inviter = getCurrentUser();
        Invitation invitation = invitationService.createInvitation(request.email(), inviter);
        return toResponse(invitation);
    }

    @GetMapping
    @Operation(summary = "List agency invitations (OWNER only)")
    public List<InvitationResponse> list() {
        UUID agencyId = TenantContext.get();
        List<Invitation> invitations = invitationService.listByAgency(agencyId);
        return invitations.stream().map(this::toResponse).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke a pending invitation (OWNER only)")
    public void revoke(@PathVariable UUID id) {
        UUID agencyId = TenantContext.get();
        invitationService.revoke(id, agencyId);
    }

    private InvitationResponse toResponse(Invitation invitation) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getToken(),
                invitationService.buildInviteUrl(invitation.getToken()),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
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
