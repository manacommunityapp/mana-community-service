package com.manacommunity.api.sms;

/**
 * Transport contract for sending SMS and WhatsApp messages.
 * Mirrors the {@code EmailService} / {@code PushNotificationService} pattern.
 */
public interface SmsService {

    /** Sends an SMS to the given phone number. */
    SmsResult sendSms(String toPhone, String body);

    /** Sends a WhatsApp message to the given phone number. */
    SmsResult sendWhatsApp(String toPhone, String body);

    /** Result of an SMS/WhatsApp send attempt. */
    record SmsResult(boolean success, String sid, String error) {
        public static SmsResult ok(String sid) { return new SmsResult(true, sid, null); }
        public static SmsResult fail(String error) { return new SmsResult(false, null, error); }
    }
}
