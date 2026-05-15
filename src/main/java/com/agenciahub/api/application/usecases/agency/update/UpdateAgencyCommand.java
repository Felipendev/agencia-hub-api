package com.agenciahub.api.application.usecases.agency.update;

import com.agenciahub.api.application.usecases.agency.update.UpdateAgencyRequestDTO;
import com.agenciahub.api.entity.PlatformAccount;

public record UpdateAgencyCommand(UpdateAgencyRequestDTO request, PlatformAccount currentUser) {
}
