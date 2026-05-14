package com.agenciahub.api.support;

import com.agenciahub.api.config.CorsConfig;
import com.agenciahub.api.config.SecurityConfig;
import com.agenciahub.api.web.GlobalExceptionHandler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Shared imports for {@code @WebMvcTest}: exception handler plus production CORS and
 * security filter-chain wiring. Use together with {@code @AutoConfigureMockMvc(addFilters = false)}
 * and {@code @MockitoBean} on {@link com.agenciahub.api.security.JwtAuthFilter} and
 * {@link com.agenciahub.api.security.RateLimitFilter} so the slice does not need a real
 * {@link com.agenciahub.api.security.JwtService}.
 * <p>For some controllers, importing only {@link com.agenciahub.api.web.GlobalExceptionHandler}
 * (without this configuration) keeps request mapping predictable in the slice; use this
 * class when the test must exercise {@code SecurityConfig} / method security together with MockMvc.
 */
@TestConfiguration
@Import({GlobalExceptionHandler.class, CorsConfig.class, SecurityConfig.class})
public class WebMvcControllerTestImports {
}
