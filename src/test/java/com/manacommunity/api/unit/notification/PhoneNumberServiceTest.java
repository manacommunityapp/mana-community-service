package com.manacommunity.api.unit.notification;

import com.manacommunity.api.exception.InvalidPhoneNumberException;
import com.manacommunity.api.notification.config.SmsProperties;
import com.manacommunity.api.notification.service.PhoneNumberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberServiceTest {

    private PhoneNumberService phoneNumberService;

    @BeforeEach
    void setUp() {
        SmsProperties props = new SmsProperties();
        props.setDefaultCountryCode("+91");
        phoneNumberService = new PhoneNumberService(props);
    }

    @Test
    @DisplayName("normalises 10-digit number to E.164 with default country code")
    void normalizesBareTenDigit() {
        assertThat(phoneNumberService.normalize("9876543210")).isEqualTo("+919876543210");
    }

    @Test
    @DisplayName("accepts already-E164 number")
    void acceptsE164() {
        assertThat(phoneNumberService.normalize("+919876543210")).isEqualTo("+919876543210");
    }

    @Test
    @DisplayName("strips spaces and dashes before normalising")
    void stripsFormatting() {
        assertThat(phoneNumberService.normalize("98765 43210")).isEqualTo("+919876543210");
    }

    @Test
    @DisplayName("throws InvalidPhoneNumberException for null input")
    void rejectsNull() {
        assertThatThrownBy(() -> phoneNumberService.normalize(null))
                .isInstanceOf(InvalidPhoneNumberException.class);
    }

    @Test
    @DisplayName("throws InvalidPhoneNumberException for too-short number")
    void rejectsShortNumber() {
        assertThatThrownBy(() -> phoneNumberService.normalize("12345"))
                .isInstanceOf(InvalidPhoneNumberException.class);
    }

    @Test
    @DisplayName("masks phone number correctly")
    void masksPhone() {
        String masked = phoneNumberService.mask("+919876543210");
        assertThat(masked).endsWith("3210");
        assertThat(masked).contains("******");
        assertThat(masked).doesNotContain("987654");
    }

    @Test
    @DisplayName("isTestNumber returns true when phone in list")
    void isTestNumber() {
        assertThat(phoneNumberService.isTestNumber("+919876543210",
                List.of("+919876543210", "+911234567890"))).isTrue();
        assertThat(phoneNumberService.isTestNumber("+919999999999",
                List.of("+919876543210"))).isFalse();
    }
}
