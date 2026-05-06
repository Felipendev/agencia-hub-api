package com.agenciahub.api.config;

import com.agenciahub.api.security.TenantInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the TenantInterceptor for all paths except public routes.
 */
@Configuration
@RequiredArgsConstructor
public class TenantInterceptorConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/auth/verify-email",
                        "/auth/resend-code",
                        "/auth/forgot-password",
                        "/auth/reset-password",
                        "/auth/invite/**",
                        "/auth/register-invite",
                        "/public/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/docs"
                );
    }
}
