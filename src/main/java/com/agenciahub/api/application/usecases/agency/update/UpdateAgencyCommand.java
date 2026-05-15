package com.agenciahub.api.application.usecases.agency.update;

import com.agenciahub.api.application.usecases.agency.update.UpdateAgencyRequestDTO;
import com.agenciahub.api.entity.User;

public record UpdateAgencyCommand(UpdateAgencyRequestDTO request, User currentUser) {
}
