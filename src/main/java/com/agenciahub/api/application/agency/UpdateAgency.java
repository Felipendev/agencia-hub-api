package com.agenciahub.api.application.agency;

import com.agenciahub.api.dto.agency.AgencyResponse;
import com.agenciahub.api.entity.Agency;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.service.AgencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateAgency implements UpdateAgencyUseCase {

    private final AgencyService agencyService;
    private final AgencyResponseMapper agencyResponseMapper;

    @Override
    public AgencyResponse execute(UpdateAgencyCommand command) {
        User user = command.currentUser();
        Agency agency =
                agencyService.update(user.getAgency().getId(), command.request(), user);
        return agencyResponseMapper.toResponse(agency);
    }
}
