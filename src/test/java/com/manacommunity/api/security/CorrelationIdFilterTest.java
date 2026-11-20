package com.manacommunity.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldExtractIncomingCorrelationIdAndEchoInResponse() throws ServletException, IOException {
        request.addHeader("X-Correlation-Id", "test-corr-12345");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("test-corr-12345");
        verify(filterChain).doFilter(request, response);
        assertThat(MDC.get("correlationId")).isNull(); // Cleared in finally
    }

    @Test
    void shouldExtractW3CTraceparentAndUseTraceIdAsFallbackCorrelationId() throws ServletException, IOException {
        String traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
        request.addHeader("traceparent", traceparent);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(response.getHeader("traceparent")).isEqualTo(traceparent);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldGenerateUUIDWhenNoHeadersProvided() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        String generatedCid = response.getHeader("X-Correlation-Id");
        assertThat(generatedCid).isNotNull().isNotBlank();
        assertThat(generatedCid.length()).isGreaterThan(20);
        verify(filterChain).doFilter(request, response);
    }
}
