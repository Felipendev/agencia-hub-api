package com.agenciahub.api.application.usecases.invitation.listinvitations;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.invitation.InvitationResponse;

import java.util.List;
import java.util.UUID;

public interface ListInvitationsUseCase extends UseCase<UUID, List<InvitationResponse>> {
}
