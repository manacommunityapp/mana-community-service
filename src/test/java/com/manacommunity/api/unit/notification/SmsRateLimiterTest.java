package com.manacommunity.api.unit.notification;

import com.manacommunity.api.exception.SmsRateLimitException;
import com.manacommunity.api.notification.config.SmsProperties;
import com.manacommunity.api.notification.service.SmsRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmsRateLimiterTest {

    private SmsRateLimiter rateLimiter;
    private SmsProperties props;

    @BeforeEach
    void setUp() {
        props = new SmsProperties();
        props.getRateLimit().setEnabled(true);
        props.getRateLimit().setPerPhonePerMinute(3);
        props.getRateLimit().setPerUserPerMinute(2);
        props.getRateLimit().setPerIpPerMinute(5);
        rateLimiter = new SmsRateLimiter(props);
    }

    // ── per-phone ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("allows up to limit requests for a phone")
    void allowsUpToPhoneLimit() {
        assertThatCode(() -> {
            rateLimiter.checkPhone("+919876543210");
            rateLimiter.checkPhone("+919876543210");
            rateLimiter.checkPhone("+919876543210");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throws SmsRateLimitException on (limit + 1)th phone request")
    void throwsAfterPhoneLimit() {
        rateLimiter.checkPhone("+919876543210");
        rateLimiter.checkPhone("+919876543210");
        rateLimiter.checkPhone("+919876543210");

        assertThatThrownBy(() -> rateLimiter.checkPhone("+919876543210"))
                .isInstanceOf(SmsRateLimitException.class);
    }

    @Test
    @DisplayName("different phones have independent buckets")
    void separatePhoneBuckets() {
        rateLimiter.checkPhone("+919876543210");
        rateLimiter.checkPhone("+919876543210");
        rateLimiter.checkPhone("+919876543210");

        // A different phone should still be allowed
        assertThatCode(() -> rateLimiter.checkPhone("+911234567890"))
                .doesNotThrowAnyException();
    }

    // ── per-user ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("allows up to limit requests for a user")
    void allowsUpToUserLimit() {
        assertThatCode(() -> {
            rateLimiter.checkUser(42L);
            rateLimiter.checkUser(42L);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throws SmsRateLimitException on (limit + 1)th user request")
    void throwsAfterUserLimit() {
        rateLimiter.checkUser(42L);
        rateLimiter.checkUser(42L);

        assertThatThrownBy(() -> rateLimiter.checkUser(42L))
                .isInstanceOf(SmsRateLimitException.class);
    }

    @Test
    @DisplayName("skips user check when userId is null")
    void nullUserIdSkipped() {
        assertThatCode(() -> rateLimiter.checkUser(null))
                .doesNotThrowAnyException();
    }

    // ── per-IP ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("throws SmsRateLimitException after IP limit")
    void throwsAfterIpLimit() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.checkIp("1.2.3.4");
        }
        assertThatThrownBy(() -> rateLimiter.checkIp("1.2.3.4"))
                .isInstanceOf(SmsRateLimitException.class);
    }

    @Test
    @DisplayName("skips IP check when ip is null")
    void nullIpSkipped() {
        assertThatCode(() -> rateLimiter.checkIp(null))
                .doesNotThrowAnyException();
    }

    // ── disabled mode ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("disabled rate limiter never throws regardless of request volume")
    void disabledNeverThrows() {
        props.getRateLimit().setEnabled(false);

        assertThatCode(() -> {
            for (int i = 0; i < 100; i++) {
                rateLimiter.checkPhone("+919876543210");
                rateLimiter.checkUser(1L);
                rateLimiter.checkIp("1.2.3.4");
            }
        }).doesNotThrowAnyException();
    }

    // ── evictStale ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("evictStale does not throw and can be called repeatedly")
    void evictStaleIsIdempotent() {
        rateLimiter.checkPhone("+919876543210");
        assertThatCode(() -> {
            rateLimiter.evictStale();
            rateLimiter.evictStale();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("independent users do not interfere with each other's limits")
    void independentUserBuckets() {
        rateLimiter.checkUser(1L);
        rateLimiter.checkUser(1L);

        // User 2 is untouched — full allowance
        assertThatCode(() -> {
            rateLimiter.checkUser(2L);
            rateLimiter.checkUser(2L);
        }).doesNotThrowAnyException();

        assertThatThrownBy(() -> rateLimiter.checkUser(1L))
                .isInstanceOf(SmsRateLimitException.class);
    }
}
