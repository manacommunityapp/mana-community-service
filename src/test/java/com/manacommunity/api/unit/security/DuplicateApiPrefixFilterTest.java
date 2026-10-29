package com.manacommunity.api.unit.security;

import com.manacommunity.api.security.DuplicateApiPrefixFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class DuplicateApiPrefixFilterTest {

    private DuplicateApiPrefixFilter filter;

    @BeforeEach
    void setUp() {
        filter = new DuplicateApiPrefixFilter();
    }

    @Test
    @DisplayName("1. Normalizes /api/api/v1/emergency/sos/trigger to /api/v1/emergency/sos/trigger")
    void testNormalizeDoubleApiEmergency() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/api/v1/emergency/sos/trigger");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(chain, times(1)).doFilter(requestCaptor.capture(), eq(response));

        HttpServletRequest forwardedRequest = requestCaptor.getValue();
        assertEquals("/api/v1/emergency/sos/trigger", forwardedRequest.getRequestURI());
        assertEquals("/api/v1/emergency/sos/trigger", forwardedRequest.getServletPath());
    }

    @Test
    @DisplayName("2. Normalizes triple /api/api/api/notices to /api/notices")
    void testNormalizeMultipleApiPrefixes() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/api/api/notices");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(chain, times(1)).doFilter(requestCaptor.capture(), eq(response));

        HttpServletRequest forwardedRequest = requestCaptor.getValue();
        assertEquals("/api/notices", forwardedRequest.getRequestURI());
        assertEquals("/api/notices", forwardedRequest.getServletPath());
    }

    @Test
    @DisplayName("3. Passes already normalized /api/v1/users cleanly without alteration")
    void testCleanApiUriUntouched() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(chain, times(1)).doFilter(requestCaptor.capture(), eq(response));

        HttpServletRequest forwardedRequest = requestCaptor.getValue();
        assertEquals("/api/v1/users", forwardedRequest.getRequestURI());
    }

    @Test
    @DisplayName("4. Static normalizeUri method handles edge cases")
    void testStaticNormalizeUri() {
        assertEquals("/api/v1/test", DuplicateApiPrefixFilter.normalizeUri("/api/api/v1/test"));
        assertEquals("/api", DuplicateApiPrefixFilter.normalizeUri("/api/api"));
        assertEquals("/api/resources", DuplicateApiPrefixFilter.normalizeUri("/api/resources"));
        assertNull(DuplicateApiPrefixFilter.normalizeUri(null));
    }
}
