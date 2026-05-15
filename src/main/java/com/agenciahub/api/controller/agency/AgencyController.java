package com.agenciahub.api.controller.agency;

import com.agenciahub.api.application.usecases.agency.getagency.GetAgencyUseCase;
import com.agenciahub.api.application.usecases.agency.updateagency.UpdateAgencyCommand;
import com.agenciahub.api.application.usecases.agency.updateagency.UpdateAgencyUseCase;
import com.agenciahub.api.application.controllers.docs.AgencyAPI;
import com.agenciahub.api.dto.agency.AgencyResponse;
import com.agenciahub.api.dto.agency.UpdateAgencyRequest;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.security.SecurityContextUsers;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class AgencyController implements AgencyAPI {

    private final GetAgencyUseCase getAgencyUseCase;
    private final UpdateAgencyUseCase updateAgencyUseCase;

    @Override
    public AgencyResponse getAgency() {
        UUID agencyId = TenantContext.requireAgencyId();
        return getAgencyUseCase.execute(agencyId);
    }

    @Override
    public AgencyResponse updateAgency(UpdateAgencyRequest request) {
        User currentUser = SecurityContextUsers.requireUser();
        return updateAgencyUseCase.execute(new UpdateAgencyCommand(request, currentUser));
    }
}
