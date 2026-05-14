package com.agenciahub.api.application.agency;

import com.agenciahub.api.dto.agency.UpdateAgencyRequest;
import com.agenciahub.api.entity.User;

public record UpdateAgencyCommand(UpdateAgencyRequest request, User currentUser) {
}
