package com.agenciahub.api.application.invitation;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.invitation.InvitationResponse;

import java.util.List;
import java.util.UUID;

public interface ListInvitationsUseCase extends UseCase<UUID, List<InvitationResponse>> {
}
