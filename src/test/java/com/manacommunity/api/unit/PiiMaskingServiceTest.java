package com.manacommunity.api.unit;

import com.manacommunity.api.privacy.PiiMaskingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PiiMaskingService}.
 * Verifies that sensitive values are correctly masked before being returned to callers.
 */
@DisplayName("PiiMaskingService")
class PiiMaskingServiceTest {

    private PiiMaskingService piiMaskingService;

    @BeforeEach
    void setUp() {
        piiMaskingService = new PiiMaskingService();
    }

    @Nested
    @DisplayName("maskPhone")
    class MaskPhone {

        @Test
        @DisplayName("keeps first 2 and last 2 digits of a 10-digit number")
        void standard10DigitNumber() {
            String masked = piiMaskingService.maskPhone("9876543210");
            assertThat(masked).startsWith("98").endsWith("10");
            assertThat(masked).doesNotContain("765432");
        }

        @Test
        @DisplayName("full phone is never present in the masked result")
        void maskedPhoneDoesNotRevealFullNumber() {
            String phone = "9876543210";
            String masked = piiMaskingService.maskPhone(phone);
            assertThat(masked).isNotEqualTo(phone);
            assertThat(masked).doesNotContain("8765432");
        }

        @Test
        @DisplayName("null or blank returns a safe placeholder")
        void nullAndBlankAreSafe() {
            assertThat(piiMaskingService.maskPhone(null)).isNotNull().isNotBlank();
            assertThat(piiMaskingService.maskPhone("")).isNotNull().isNotBlank();
        }
    }

    @Nested
    @DisplayName("maskEmail")
    class MaskEmail {

        @Test
        @DisplayName("keeps first chars and domain, hides local part middle")
        void standardEmail() {
            String masked = piiMaskingService.maskEmail("sandeep@gmail.com");
            assertThat(masked).contains("@gmail.com");
            assertThat(masked).doesNotContain("sandeep");
        }

        @Test
        @DisplayName("masked email never equals the original")
        void maskedEmailIsNotOriginal() {
            String email = "user@example.com";
            assertThat(piiMaskingService.maskEmail(email)).isNotEqualTo(email);
        }

        @Test
        @DisplayName("null or blank returns safe placeholder")
        void nullAndBlankAreSafe() {
            assertThat(piiMaskingService.maskEmail(null)).isNotNull().isNotBlank();
        }
    }

    @Nested
    @DisplayName("maskName")
    class MaskName {

        @Test
        @DisplayName("keeps first name and surname initial only")
        void fullThreePartName() {
            String masked = piiMaskingService.maskName("Rajesh Kumar Sharma");
            assertThat(masked).startsWith("Rajesh");
            assertThat(masked).contains("S.");
            assertThat(masked).doesNotContain("Kumar");
        }

        @Test
        @DisplayName("single word name is returned as-is")
        void singleWordName() {
            assertThat(piiMaskingService.maskName("Raju")).isEqualTo("Raju");
        }

        @Test
        @DisplayName("null returns safe placeholder")
        void nullNameIsSafe() {
            assertThat(piiMaskingService.maskName(null)).isNotNull().isNotBlank();
        }
    }

    @Nested
    @DisplayName("maskVehicleNumber")
    class MaskVehicleNumber {

        @Test
        @DisplayName("keeps state code prefix and last 2 chars")
        void standardVehicleNumber() {
            String masked = piiMaskingService.maskVehicleNumber("TS09AB1234");
            assertThat(masked).startsWith("TS09");
            assertThat(masked).endsWith("34");
            assertThat(masked).doesNotContain("AB12");
        }

        @Test
        @DisplayName("null or blank returns safe placeholder")
        void nullVehicleNumberIsSafe() {
            assertThat(piiMaskingService.maskVehicleNumber(null)).isNotNull().isNotBlank();
        }
    }

    @Nested
    @DisplayName("maskFlat")
    class MaskFlat {

        @Test
        @DisplayName("keeps block letter prefix only, masks unit number")
        void standardFlatNumber() {
            String masked = piiMaskingService.maskFlat("B-402");
            assertThat(masked).startsWith("B");
            assertThat(masked).doesNotContain("402");
        }

        @Test
        @DisplayName("null flat returns safe placeholder")
        void nullFlatIsSafe() {
            assertThat(piiMaskingService.maskFlat(null)).isNotNull().isNotBlank();
        }
    }

    @Nested
    @DisplayName("redact")
    class Redact {

        @Test
        @DisplayName("fully replaces value with REDACTED marker")
        void anyValueIsFullyRedacted() {
            // MaskingUtil.REDACTED = "***"
            assertThat(piiMaskingService.redact("SensitivePassword123!")).isEqualTo("***");
            assertThat(piiMaskingService.redact("1234567890")).isEqualTo("***");
        }

        @Test
        @DisplayName("null or blank also returns REDACTED marker")
        void nullIsRedacted() {
            assertThat(piiMaskingService.redact(null)).isNotNull();
        }
    }
}
