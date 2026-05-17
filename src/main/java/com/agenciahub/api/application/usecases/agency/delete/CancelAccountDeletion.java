package com.agenciahub.api.application.usecases.agency.delete;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelAccountDeletion {

    private final AgencyRepository agencyRepository;

    @Transactional
    public void execute(CancelAccountDeletionRequestDTO request) {
        Agency agency = agencyRepository.findById(request.agencyId())
                .orElseThrow(() -> new ResourceNotFoundException("agência não encontrada"));

        if (agency.getStatus() != AgencyStatus.DELETION_PENDING) {
            throw new IllegalStateException("conta não está em processo de exclusão");
        }

        AgencyStatus previousStatus;
        try {
            previousStatus = agency.getStatusBeforeDeletion() != null
                    ? AgencyStatus.valueOf(agency.getStatusBeforeDeletion())
                    : AgencyStatus.TRIAL;
        } catch (IllegalArgumentException e) {
            previousStatus = AgencyStatus.TRIAL;
        }

        agency.setStatus(previousStatus);
        agency.setDeletionScheduledAt(null);
        agency.setStatusBeforeDeletion(null);

        agencyRepository.save(agency);
    }
}
