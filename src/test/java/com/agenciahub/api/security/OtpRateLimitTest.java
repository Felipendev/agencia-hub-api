package com.agenciahub.api.security;

import com.agenciahub.api.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * SEC-02: rate limit específico de 5 req/min por IP para endpoints OTP.
 */
class OtpRateLimitTest {

    private static final int OTP_LIMIT = 5;
    private static final String TEST_IP = "203.0.113.50";

    private static RateLimitFilter buildFilter() {
        RateLimitProperties props = new RateLimitProperties();
        props.setEnabled(true);
        props.setTrustXForwardedFor(false);
        props.setAuthVerifyEmailPerMinute(OTP_LIMIT);
        props.setAuthResetPasswordPerMinute(OTP_LIMIT);
        RateLimitFilter filter = new RateLimitFilter(props, new ObjectMapper());
        filter.initRules();
        return filter;
    }

    private static MockHttpServletRequest postRequest(String path, String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod("POST");
        req.setRequestURI(path);
        req.setContextPath("");
        req.setServletPath(path);
        req.setRemoteAddr(ip);
        return req;
    }

    // ── /auth/verify-email ─────────────────────────────────────────────────────

    @Test
    void verifyEmail_allowsUpToLimit() throws Exception {
        RateLimitFilter filter = buildFilter();
        FilterChain chain = mock(FilterChain.class);
        for (int i = 0; i < OTP_LIMIT; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(postRequest("/auth/verify-email", TEST_IP), res, chain);
            assertNotEquals(429, res.getStatus(), "request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void verifyEmail_blocksAfterLimit() throws Exception {
        RateLimitFilter filter = buildFilter();
        FilterChain chain = mock(FilterChain.class);
        // exhaust the bucket
        for (int i = 0; i < OTP_LIMIT; i++) {
            filter.doFilter(postRequest("/auth/verify-email", TEST_IP), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(postRequest("/auth/verify-email", TEST_IP), blocked, chain);
        assertEquals(429, blocked.getStatus());
        assertNotNull(blocked.getHeader("Retry-After"));
        assertTrue(blocked.getContentAsString().contains("TOO_MANY_REQUESTS"));
    }

    @Test
    void verifyEmail_differentIpsHaveIndependentBuckets() throws Exception {
        RateLimitFilter filter = buildFilter();
        FilterChain chain = mock(FilterChain.class);

        String ip1 = "203.0.113.51";
        String ip2 = "203.0.113.52";
        // exhaust ip1
        for (int i = 0; i < OTP_LIMIT; i++) {
            filter.doFilter(postRequest("/auth/verify-email", ip1), new MockHttpServletResponse(), chain);
        }

        // ip2 should still be allowed
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        filter.doFilter(postRequest("/auth/verify-email", ip2), res2, chain);
        assertNotEquals(429, res2.getStatus());
    }

    // ── /auth/reset-password ──────────────────────────────────────────────────

    @Test
    void resetPassword_blocksAfterLimit() throws Exception {
        RateLimitFilter filter = buildFilter();
        FilterChain chain = mock(FilterChain.class);
        for (int i = 0; i < OTP_LIMIT; i++) {
            filter.doFilter(postRequest("/auth/reset-password", TEST_IP), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(postRequest("/auth/reset-password", TEST_IP), blocked, chain);
        assertEquals(429, blocked.getStatus());
        assertNotNull(blocked.getHeader("Retry-After"));
    }

    @Test
    void verifyEmailAndResetPassword_haveSeparateBuckets() throws Exception {
        RateLimitFilter filter = buildFilter();
        FilterChain chain = mock(FilterChain.class);
        // exhaust verify-email
        for (int i = 0; i < OTP_LIMIT; i++) {
            filter.doFilter(postRequest("/auth/verify-email", TEST_IP), new MockHttpServletResponse(), chain);
        }
        // reset-password on same IP should still have its own independent bucket
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(postRequest("/auth/reset-password", TEST_IP), res, chain);
        assertNotEquals(429, res.getStatus());
    }
}
