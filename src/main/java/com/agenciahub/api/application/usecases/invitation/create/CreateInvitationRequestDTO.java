package com.agenciahub.api.application.usecases.invitation.create;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateInvitationRequestDTO(
        @NotBlank @Email String email
) {
}
