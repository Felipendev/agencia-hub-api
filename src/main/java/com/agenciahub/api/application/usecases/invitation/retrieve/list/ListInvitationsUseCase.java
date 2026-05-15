package com.agenciahub.api.application.usecases.invitation.retrieve.list;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.invitation.shared.InvitationSummaryResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ListInvitationsUseCase extends UseCase<UUID, List<InvitationSummaryResponseDTO>> {
}
