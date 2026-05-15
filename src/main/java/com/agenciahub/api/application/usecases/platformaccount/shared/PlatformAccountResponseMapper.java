package com.agenciahub.api.application.usecases.platformaccount.shared;

import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountSummaryResponseDTO;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import org.springframework.stereotype.Component;

@Component
public class PlatformAccountResponseMapper {

    public PlatformAccountSummaryResponseDTO toResponse(PlatformAccount u) {
        return new PlatformAccountSummaryResponseDTO(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getAccountKind(),
                Boolean.TRUE.equals(u.getActive()),
                u.getCommissionPct(),
                u.getCommissionFixed(),
                u.getCreatedAt(),
                Boolean.TRUE.equals(u.getTermsAccepted()));
    }
}
