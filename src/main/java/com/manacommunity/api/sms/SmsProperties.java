package com.manacommunity.api.sms;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds {@code app.sms.*} settings for Twilio SMS and WhatsApp messaging.
 *
 * <p>Follows the same enable/disable pattern as {@code EmailProperties}: when
 * {@code enabled} is false, messages are logged but never dispatched — safe for
 * dev/test environments.</p>
 */
@Component
@ConfigurationProperties(prefix = "app.sms")
@Getter
@Setter
public class SmsProperties {

    /** Master switch. When false, SMS/WhatsApp messages are logged but not sent. */
    private boolean enabled = false;

    /** Twilio Account SID. */
    private String accountSid;

    /** Twilio Auth Token. */
    private String authToken;

    /** Twilio phone number for SMS (e.g. "+1234567890"). */
    private String fromNumber;

    /** Twilio WhatsApp sender (e.g. "whatsapp:+14155238886"). */
    private String whatsappFrom;

    /** Default country code prefix when user's phone doesn't start with '+' (e.g. "+91"). */
    private String defaultCountryCode = "+91";
}
