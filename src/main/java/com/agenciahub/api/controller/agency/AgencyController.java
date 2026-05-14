package com.agenciahub.api.controller.agency;

import com.agenciahub.api.application.agency.GetAgencyUseCase;
import com.agenciahub.api.application.agency.UpdateAgencyCommand;
import com.agenciahub.api.application.agency.UpdateAgencyUseCase;
import com.agenciahub.api.controller.agency.docs.AgencyAPI;
import com.agenciahub.api.dto.agency.AgencyResponse;
import com.agenciahub.api.dto.agency.UpdateAgencyRequest;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        UUID agencyId = TenantContext.get();
        return getAgencyUseCase.execute(agencyId);
    }

    @Override
    public AgencyResponse updateAgency(UpdateAgencyRequest request) {
        User currentUser = getCurrentUser();
        return updateAgencyUseCase.execute(new UpdateAgencyCommand(request, currentUser));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof User user) {
            return user;
        }
        throw new ResourceNotFoundException("usuário não encontrado");
    }
}
