package com.agenciahub.api.support;

import com.agenciahub.api.config.CorsConfig;
import com.agenciahub.api.config.SecurityConfig;
import com.agenciahub.api.controller.GlobalExceptionHandler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Shared imports for {@code @WebMvcTest}: exception handler plus production CORS and
 * security filter-chain wiring. Use together with {@code @AutoConfigureMockMvc(addFilters = false)}
 * and {@code @MockitoBean} on {@link com.agenciahub.api.security.JwtAuthFilter} and
 * {@link com.agenciahub.api.security.RateLimitFilter} so the slice does not need a real
 * {@link com.agenciahub.api.security.JwtService}.
 */
@TestConfiguration
@Import({GlobalExceptionHandler.class, CorsConfig.class, SecurityConfig.class})
public class WebMvcControllerTestImports {
}
