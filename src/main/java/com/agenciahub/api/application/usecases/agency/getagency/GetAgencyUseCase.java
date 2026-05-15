package com.agenciahub.api.application.usecases.agency.getagency;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.agency.AgencyResponse;

import java.util.UUID;

public interface GetAgencyUseCase extends UseCase<UUID, AgencyResponse> {
}
