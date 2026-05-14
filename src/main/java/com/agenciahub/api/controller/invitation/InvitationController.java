package com.agenciahub.api.controller.invitation;

import com.agenciahub.api.application.invitation.CreateInvitationCommand;
import com.agenciahub.api.application.invitation.CreateInvitationUseCase;
import com.agenciahub.api.application.invitation.ListInvitationsUseCase;
import com.agenciahub.api.application.invitation.RevokeInvitationCommand;
import com.agenciahub.api.application.invitation.RevokeInvitationUseCase;
import com.agenciahub.api.controller.invitation.docs.InvitationAPI;
import com.agenciahub.api.dto.invitation.CreateInvitationRequest;
import com.agenciahub.api.dto.invitation.InvitationResponse;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class InvitationController implements InvitationAPI {

    private final CreateInvitationUseCase createInvitationUseCase;
    private final ListInvitationsUseCase listInvitationsUseCase;
    private final RevokeInvitationUseCase revokeInvitationUseCase;

    @Override
    public InvitationResponse create(CreateInvitationRequest request) {
        User inviter = getCurrentUser();
        return createInvitationUseCase.execute(new CreateInvitationCommand(request, inviter));
    }

    @Override
    public List<InvitationResponse> list() {
        UUID agencyId = TenantContext.get();
        return listInvitationsUseCase.execute(agencyId);
    }

    @Override
    public void revoke(UUID id) {
        UUID agencyId = TenantContext.get();
        revokeInvitationUseCase.execute(new RevokeInvitationCommand(id, agencyId));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof User user) {
            return user;
        }
        throw new ResourceNotFoundException("usuário não encontrado");
    }
}
