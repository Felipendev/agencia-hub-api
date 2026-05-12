package com.agenciahub.api.security;

import com.agenciahub.api.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RateLimitFilterTest {

    private static MockHttpServletRequest postPublicSubmit(String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod("POST");
        req.setRequestURI("/public/solicitacao/submit");
        req.setContextPath("");
        req.setServletPath("/public/solicitacao/submit");
        req.setRemoteAddr(ip);
        return req;
    }

    @Test
    void publicSubmit_blocksAfterCapacity() throws Exception {
        RateLimitProperties props = new RateLimitProperties();
        props.setEnabled(true);
        props.setTrustXForwardedFor(false);
        props.setPublicSubmitPerMinute(2);

        RateLimitFilter filter = new RateLimitFilter(props, new ObjectMapper());
        filter.initRules();

        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 2; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(postPublicSubmit("203.0.113.10"), res, chain);
            assertNotEquals(429, res.getStatus());
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(postPublicSubmit("203.0.113.10"), blocked, chain);
        assertEquals(429, blocked.getStatus());
        assertTrue(blocked.getHeader("Retry-After") != null && !blocked.getHeader("Retry-After").isBlank());
        assertTrue(blocked.getContentAsString().contains("TOO_MANY_REQUESTS"));
    }

    @Test
    void differentIpsHaveSeparateBuckets() throws Exception {
        RateLimitProperties props = new RateLimitProperties();
        props.setEnabled(true);
        props.setPublicSubmitPerMinute(1);

        RateLimitFilter filter = new RateLimitFilter(props, new ObjectMapper());
        filter.initRules();

        FilterChain chain = mock(FilterChain.class);

        MockHttpServletResponse r1 = new MockHttpServletResponse();
        filter.doFilter(postPublicSubmit("203.0.113.20"), r1, chain);
        assertNotEquals(429, r1.getStatus());

        MockHttpServletResponse r2 = new MockHttpServletResponse();
        filter.doFilter(postPublicSubmit("203.0.113.21"), r2, chain);
        assertNotEquals(429, r2.getStatus());
    }
}
