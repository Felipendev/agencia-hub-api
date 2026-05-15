package com.agenciahub.api.application.usecases.user.shared;

import com.agenciahub.api.application.usecases.user.shared.UserSummaryResponseDTO;
import com.agenciahub.api.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserResponseMapper {

    public UserSummaryResponseDTO toResponse(User u) {
        return new UserSummaryResponseDTO(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getRole(),
                Boolean.TRUE.equals(u.getActive()),
                u.getCommissionPct(),
                u.getCommissionFixed(),
                u.getCreatedAt(),
                Boolean.TRUE.equals(u.getTermsAccepted()));
    }
}
