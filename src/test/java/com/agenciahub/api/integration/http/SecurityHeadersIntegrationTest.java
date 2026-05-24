package com.agenciahub.api.integration.http;

import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SEC-07: verifica presença dos headers de segurança em endpoints públicos e autenticados.
 */
class SecurityHeadersIntegrationTest extends AbstractIntegrationTest {

    @Test
    void publicEndpoint_hasXFrameOptions() throws Exception {
        mockMvc.perform(post(IntegrationHttpSupport.API_PREFIX + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@agenciahub.com","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void publicEndpoint_hasXContentTypeOptions() throws Exception {
        mockMvc.perform(post(IntegrationHttpSupport.API_PREFIX + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@agenciahub.com","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void publicEndpoint_hasReferrerPolicy() throws Exception {
        mockMvc.perform(post(IntegrationHttpSupport.API_PREFIX + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@agenciahub.com","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    void publicEndpoint_hasContentSecurityPolicy() throws Exception {
        mockMvc.perform(post(IntegrationHttpSupport.API_PREFIX + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@agenciahub.com","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void contentSecurityPolicy_doesNotContainUnsafeInlineForScripts() throws Exception {
        mockMvc.perform(post(IntegrationHttpSupport.API_PREFIX + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@agenciahub.com","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy",
                        not(containsString("script-src 'unsafe-inline'"))));
    }

    @Test
    void authenticatedEndpoint_hasSecurityHeaders() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/customers")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().exists("Content-Security-Policy"));
    }
}
