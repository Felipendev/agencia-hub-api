package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.invitation.create.CreateInvitationCommand;
import com.agenciahub.api.application.usecases.invitation.create.CreateInvitationUseCase;
import com.agenciahub.api.application.usecases.invitation.retrieve.list.ListInvitationsUseCase;
import com.agenciahub.api.application.usecases.invitation.revoke.RevokeInvitationCommand;
import com.agenciahub.api.application.usecases.invitation.revoke.RevokeInvitationUseCase;
import com.agenciahub.api.application.controller.doc.InvitationAPI;
import com.agenciahub.api.application.usecases.invitation.create.CreateInvitationRequestDTO;
import com.agenciahub.api.application.usecases.invitation.shared.InvitationSummaryResponseDTO;
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
    public InvitationSummaryResponseDTO create(CreateInvitationRequestDTO request) {
        User inviter = SecurityContextUsers.requireUser();
        return createInvitationUseCase.execute(new CreateInvitationCommand(request, inviter));
    }

    @Override
    public List<InvitationSummaryResponseDTO> list() {
        UUID agencyId = TenantContext.requireAgencyId();
        return listInvitationsUseCase.execute(agencyId);
    }

    @Override
    public void revoke(UUID id) {
        UUID agencyId = TenantContext.requireAgencyId();
        revokeInvitationUseCase.execute(new RevokeInvitationCommand(id, agencyId));
    }
}
