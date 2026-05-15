package com.agenciahub.api.application.usecases.user;

import com.agenciahub.api.dto.user.UserResponse;
import com.agenciahub.api.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserResponseMapper {

    public UserResponse toResponse(User u) {
        return new UserResponse(
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
