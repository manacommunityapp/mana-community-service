package com.manacommunity.api.privacy;

import com.manacommunity.api.security.MaskingUtil;
import org.springframework.stereotype.Service;

/**
 * Centralized PII masking service. Inject this @Service in all modules instead
 * of calling MaskingUtil static methods directly, so masking logic is testable
 * and consistently applied across the application.
 *
 * <p>Masking is a <em>display-layer</em> control, not a storage control.
 * Backend must always mask before sending PII to lower-trust callers.
 */
@Service
public class PiiMaskingService {

    /**
     * Masks a phone/mobile number, keeping first 2 and last 2 digits.
     * Example: 9876543210 → 98******10
     */
    public String maskPhone(String phone) {
        return MaskingUtil.maskMobile(phone);
    }

    /**
     * Masks an email address.
     * Example: rajesh@gmail.com → ra*****@gmail.com
     */
    public String maskEmail(String email) {
        return MaskingUtil.maskEmail(email);
    }

    /**
     * Masks a vehicle registration number, keeping first 4 and last 2 characters.
     * Example: TS09AB1234 → TS09****34
     */
    public String maskVehicleNumber(String vehicleNumber) {
        if (vehicleNumber == null || vehicleNumber.isBlank()) return "-";
        String v = vehicleNumber.trim();
        if (v.length() <= 6) return MaskingUtil.REDACTED;
        return MaskingUtil.maskKeepingEnds(v, 4, 2);
    }

    /**
     * Masks a person's full name to first name + surname initial.
     * Example: "Rajesh Kumar Sharma" → "Rajesh S."
     */
    public String maskName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "-";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }

    /**
     * Masks a flat/unit number, keeping only the block prefix.
     * Example: "B-402" → "B-***", "A101" → "A***"
     */
    public String maskFlat(String flatNumber) {
        if (flatNumber == null || flatNumber.isBlank()) return "-";
        String f = flatNumber.trim();
        if (f.length() <= 2) return MaskingUtil.REDACTED;
        // Find the first digit position to keep only alpha prefix
        int firstDigit = -1;
        for (int i = 0; i < f.length(); i++) {
            if (Character.isDigit(f.charAt(i))) {
                firstDigit = i;
                break;
            }
        }
        if (firstDigit <= 0) return MaskingUtil.maskKeepingEnds(f, 1, 0);
        return f.substring(0, firstDigit) + "***";
    }

    /**
     * Fully redacts a sensitive value, replacing with [REDACTED].
     */
    public String redact(String value) {
        return MaskingUtil.redact(value);
    }
}
