package com.manacommunity.api.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.Map;

/**
 * Twilio SMS and WhatsApp implementation using Spring's {@link RestClient}
 * (built into Boot 4 — no Twilio SDK dependency needed).
 *
 * <p>Gated by {@code app.sms.enabled}. When disabled, messages are fully formatted
 * and logged but never dispatched — identical to the email stub pattern.</p>
 *
 * <p>WhatsApp messages use Twilio's WhatsApp sandbox/business API by prefixing
 * the recipient number with {@code whatsapp:}.</p>
 */
@Slf4j
@Service
public class TwilioSmsService implements SmsService {

    private static final String TWILIO_API = "https://api.twilio.com/2010-04-01/Accounts";

    private final SmsProperties props;
    private final RestClient restClient;

    public TwilioSmsService(SmsProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(TWILIO_API)
                .build();
    }

    @Override
    public SmsResult sendSms(String toPhone, String body) {
        String normalizedPhone = normalizePhone(toPhone);
        if (normalizedPhone == null) {
            return SmsResult.fail("Invalid phone number");
        }

        if (!props.isEnabled() || isBlank(props.getAccountSid())) {
            log.info("[SMS DISABLED] Would send to {}: {}", normalizedPhone, truncate(body));
            return SmsResult.ok("STUB-" + System.currentTimeMillis());
        }

        return doSend(props.getFromNumber(), normalizedPhone, body, "SMS");
    }

    @Override
    public SmsResult sendWhatsApp(String toPhone, String body) {
        String normalizedPhone = normalizePhone(toPhone);
        if (normalizedPhone == null) {
            return SmsResult.fail("Invalid phone number");
        }

        if (!props.isEnabled() || isBlank(props.getAccountSid())) {
            log.info("[WHATSAPP DISABLED] Would send to {}: {}", normalizedPhone, truncate(body));
            return SmsResult.ok("STUB-" + System.currentTimeMillis());
        }

        String whatsappTo = "whatsapp:" + normalizedPhone;
        String whatsappFrom = props.getWhatsappFrom() != null
                ? props.getWhatsappFrom()
                : "whatsapp:" + props.getFromNumber();

        return doSend(whatsappFrom, whatsappTo, body, "WhatsApp");
    }

    // ── Twilio REST API call ───────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private SmsResult doSend(String from, String to, String body, String channel) {
        try {
            String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                    (props.getAccountSid() + ":" + props.getAuthToken()).getBytes());

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("From", from);
            form.add("To", to);
            form.add("Body", body);

            Map<String, Object> response = restClient.post()
                    .uri("/{sid}/Messages.json", props.getAccountSid())
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            String sid = response != null ? (String) response.get("sid") : "unknown";
            log.info("[{} SENT] -> {} (SID: {})", channel, to, sid);
            return SmsResult.ok(sid);
        } catch (Exception e) {
            log.error("[{} FAILED] -> {}: {}", channel, to, e.getMessage(), e);
            return SmsResult.fail(e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private String normalizePhone(String phone) {
        if (isBlank(phone)) return null;
        String cleaned = phone.replaceAll("[^+\\d]", "");
        if (cleaned.isEmpty()) return null;
        if (!cleaned.startsWith("+")) {
            cleaned = props.getDefaultCountryCode() + cleaned;
        }
        return cleaned;
    }

    private String truncate(String s) {
        return s != null && s.length() > 100 ? s.substring(0, 100) + "..." : s;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
