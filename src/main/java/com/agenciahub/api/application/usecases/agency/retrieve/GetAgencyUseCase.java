package com.agenciahub.api.application.usecases.agency.retrieve;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.agency.shared.AgencySummaryResponseDTO;

import java.util.UUID;

public interface GetAgencyUseCase extends UseCase<UUID, AgencySummaryResponseDTO> {
}
