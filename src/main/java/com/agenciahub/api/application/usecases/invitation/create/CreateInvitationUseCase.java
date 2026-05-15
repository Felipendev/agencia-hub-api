package com.agenciahub.api.application.usecases.invitation.create;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.invitation.shared.InvitationSummaryResponseDTO;

public interface CreateInvitationUseCase extends UseCase<CreateInvitationCommand, InvitationSummaryResponseDTO> {
}
