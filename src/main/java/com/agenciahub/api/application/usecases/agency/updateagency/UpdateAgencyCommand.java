package com.agenciahub.api.application.usecases.agency.updateagency;

import com.agenciahub.api.dto.agency.UpdateAgencyRequest;
import com.agenciahub.api.entity.User;

public record UpdateAgencyCommand(UpdateAgencyRequest request, User currentUser) {
}
