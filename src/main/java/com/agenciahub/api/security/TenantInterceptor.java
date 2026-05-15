package com.agenciahub.api.security;

import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that extracts the agency_id from the authenticated user
 * and sets it in TenantContext for the duration of the request.
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        SecurityContextUsers.optionalUser()
                .map(PlatformAccount::getAgency)
                .ifPresent(agency -> TenantContext.set(agency.getId()));

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        TenantContext.clear();
    }
}
