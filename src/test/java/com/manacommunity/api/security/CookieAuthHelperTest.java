package com.manacommunity.api.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CookieAuthHelper Unit Tests")
class CookieAuthHelperTest {

    private CookieAuthHelper helper;

    @BeforeEach
    void setUp() {
        // accessExpirationMs = 900,000 (15 min), refreshExpirationMs = 604,800,000 (7 days), secure = true
        helper = new CookieAuthHelper(900_000L, 604_800_000L, true, "", "Lax");
    }

    @Test
    @DisplayName("setAuthCookies emits HttpOnly access and refresh cookies, and readable XSRF cookie")
    void setAuthCookies_emitsExpectedCookies() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        helper.setAuthCookies(response, "mock-access-token", "mock-refresh-token");

        List<String> setCookies = response.getHeaders("Set-Cookie");
        assertThat(setCookies).hasSize(3);

        String accessCookie = setCookies.stream().filter(c -> c.startsWith("access_token=")).findFirst().orElse(null);
        assertThat(accessCookie).isNotNull();
        assertThat(accessCookie).contains("HttpOnly");
        assertThat(accessCookie).contains("SameSite=Lax");
        assertThat(accessCookie).contains("mock-access-token");

        String refreshCookie = setCookies.stream().filter(c -> c.startsWith("refresh_token=")).findFirst().orElse(null);
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie).contains("HttpOnly");
        assertThat(refreshCookie).contains("SameSite=Lax");
        assertThat(refreshCookie).contains("mock-refresh-token");

        String csrfCookie = setCookies.stream().filter(c -> c.startsWith("XSRF-TOKEN=")).findFirst().orElse(null);
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie).doesNotContain("HttpOnly"); // Frontend JS must be able to read it
        assertThat(csrfCookie).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("clearAuthCookies expires all auth cookies with maxAge=0")
    void clearAuthCookies_clearsAllCookies() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        helper.clearAuthCookies(response);

        List<String> setCookies = response.getHeaders("Set-Cookie");
        assertThat(setCookies).hasSize(3);

        for (String cookie : setCookies) {
            assertThat(cookie).contains("Max-Age=0");
        }
    }

    @Test
    @DisplayName("extractAccessToken and extractRefreshToken extract tokens correctly")
    void extractTokens_fromRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie("access_token", "sample-access"),
                new Cookie("refresh_token", "sample-refresh"),
                new Cookie("XSRF-TOKEN", "sample-csrf")
        );
        request.addHeader("X-XSRF-TOKEN", "sample-csrf");

        assertThat(helper.extractAccessToken(request)).isEqualTo("sample-access");
        assertThat(helper.extractRefreshToken(request)).isEqualTo("sample-refresh");
        assertThat(helper.getCsrfCookie(request)).isEqualTo("sample-csrf");
        assertThat(helper.getCsrfHeader(request)).isEqualTo("sample-csrf");
    }
}
