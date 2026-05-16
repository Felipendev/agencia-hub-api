package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.agency.retrieve.GetAgencyUseCase;
import com.agenciahub.api.application.usecases.agency.update.UpdateAgencyCommand;
import com.agenciahub.api.application.usecases.agency.update.UpdateAgencyUseCase;
import com.agenciahub.api.application.controller.doc.AgencyAPI;
import com.agenciahub.api.application.usecases.agency.shared.AgencySummaryResponseDTO;
import com.agenciahub.api.application.usecases.agency.update.UpdateAgencyRequestDTO;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.security.SecurityContextUsers;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGENCY_OWNER')")
public class AgencyController implements AgencyAPI {

    private final GetAgencyUseCase getAgencyUseCase;
    private final UpdateAgencyUseCase updateAgencyUseCase;

    @Override
    public AgencySummaryResponseDTO getAgency() {
        UUID agencyId = TenantContext.requireAgencyId();
        return getAgencyUseCase.execute(agencyId);
    }

    @Override
    public AgencySummaryResponseDTO updateAgency(UpdateAgencyRequestDTO request) {
        PlatformAccount currentUser = SecurityContextUsers.requireUser();
        return updateAgencyUseCase.execute(new UpdateAgencyCommand(request, currentUser));
    }
}
