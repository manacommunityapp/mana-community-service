package com.manacommunity.api.notification.service;

import com.manacommunity.api.exception.InvalidPhoneNumberException;
import com.manacommunity.api.notification.config.SmsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PhoneNumberService {

    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");
    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d{10}$");

    private final SmsProperties smsProperties;

    /** Normalises a phone number to E.164 format. Throws if it cannot be normalised. */
    public String normalize(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new InvalidPhoneNumberException(phone);
        }
        String cleaned = phone.replaceAll("[\\s\\-()]", "");

        if (E164_PATTERN.matcher(cleaned).matches()) {
            return cleaned;
        }
        // 10-digit bare number — prepend default country code
        if (DIGITS_ONLY.matcher(cleaned).matches()) {
            return smsProperties.getDefaultCountryCode() + cleaned;
        }
        throw new InvalidPhoneNumberException(phone);
    }

    public void validate(String phone) {
        normalize(phone); // throws if invalid
    }

    /** Masks to `+91******1234` for logging / display */
    public String mask(String normalizedPhone) {
        if (normalizedPhone == null || normalizedPhone.length() < 5) return "****";
        int keep = 4;
        String prefix = normalizedPhone.substring(0, normalizedPhone.length() - keep - 6);
        String suffix = normalizedPhone.substring(normalizedPhone.length() - keep);
        return prefix + "******" + suffix;
    }

    public boolean isTestNumber(String normalizedPhone, java.util.List<String> testNumbers) {
        return testNumbers != null && testNumbers.contains(normalizedPhone);
    }
}
