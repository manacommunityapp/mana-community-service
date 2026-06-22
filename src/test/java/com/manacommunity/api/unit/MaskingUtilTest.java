package com.manacommunity.api.unit;

import com.manacommunity.api.security.MaskingUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that sensitive values are masked and never leak in clear text.
 */
class MaskingUtilTest {

    @Test
    void maskEmail_keepsFirstTwoCharsAndDomain() {
        assertThat(MaskingUtil.maskEmail("sandeep@gmail.com")).isEqualTo("sa*****@gmail.com");
        assertThat(MaskingUtil.maskEmail("ab@x.com")).isEqualTo("a*@x.com");
        assertThat(MaskingUtil.maskEmail("a@x.com")).isEqualTo("a*@x.com");
    }

    @Test
    void maskMobile_keepsFirstTwoAndLastTwoDigits() {
        assertThat(MaskingUtil.maskMobile("9876543210")).isEqualTo("98******10");
        assertThat(MaskingUtil.maskMobile("+91 98765 43210")).isEqualTo("91********10");
    }

    @Test
    void maskAadhaar_keepsOnlyLastFour() {
        assertThat(MaskingUtil.maskAadhaar("1234 5678 9012")).isEqualTo("********9012");
    }

    @Test
    void maskToken_neverRevealsFullSecret() {
        String jwt = "eyJhbGciOiJIUzI1NiIsInverylongtoken";
        assertThat(MaskingUtil.maskToken(jwt)).doesNotContain("verylongtoken");
        assertThat(MaskingUtil.maskToken("short")).isEqualTo(MaskingUtil.REDACTED);
    }

    @Test
    void redact_fullyHidesValue() {
        assertThat(MaskingUtil.redact("Password123!")).isEqualTo(MaskingUtil.REDACTED);
        assertThat(MaskingUtil.redact("123456")).isEqualTo(MaskingUtil.REDACTED);
    }

    @Test
    void nullAndBlankAreSafe() {
        assertThat(MaskingUtil.maskEmail(null)).isEqualTo("-");
        assertThat(MaskingUtil.maskMobile("")).isEqualTo("-");
        assertThat(MaskingUtil.maskAadhaar("   ")).isEqualTo("-");
        assertThat(MaskingUtil.redact(null)).isEqualTo("-");
    }
}
