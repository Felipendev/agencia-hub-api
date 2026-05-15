package com.agenciahub.api.controller.invitation;

import com.agenciahub.api.application.usecases.invitation.createinvitation.CreateInvitationCommand;
import com.agenciahub.api.application.usecases.invitation.createinvitation.CreateInvitationUseCase;
import com.agenciahub.api.application.usecases.invitation.listinvitations.ListInvitationsUseCase;
import com.agenciahub.api.application.usecases.invitation.revokeinvitation.RevokeInvitationCommand;
import com.agenciahub.api.application.usecases.invitation.revokeinvitation.RevokeInvitationUseCase;
import com.agenciahub.api.application.controllers.docs.InvitationAPI;
import com.agenciahub.api.dto.invitation.CreateInvitationRequest;
import com.agenciahub.api.dto.invitation.InvitationResponse;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.security.SecurityContextUsers;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
        User inviter = SecurityContextUsers.requireUser();
        return createInvitationUseCase.execute(new CreateInvitationCommand(request, inviter));
    }

    @Override
    public List<InvitationResponse> list() {
        UUID agencyId = TenantContext.requireAgencyId();
        return listInvitationsUseCase.execute(agencyId);
    }

    @Override
    public void revoke(UUID id) {
        UUID agencyId = TenantContext.requireAgencyId();
        revokeInvitationUseCase.execute(new RevokeInvitationCommand(id, agencyId));
    }
}
