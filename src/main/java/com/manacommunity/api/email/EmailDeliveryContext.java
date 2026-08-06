package com.manacommunity.api.email;

import org.slf4j.MDC;

/**
 * Utility for tagging email delivery log entries with template type and community context.
 *
 * <p>Because {@link SmtpEmailService#send(EmailMessage)} reads context from
 * {@link org.slf4j.MDC} (to avoid adding parameters to the existing {@link EmailMessage}
 * record), callers that want their sends labelled in the admin delivery log should wrap
 * their {@code send()} calls with {@link #withContext(String, Long, Runnable)}:</p>
 *
 * <pre>{@code
 * EmailDeliveryContext.withContext("OTP", communityId,
 *     () -> emailService.send(message));
 * }</pre>
 *
 * <p>The MDC keys are always cleaned up via try-finally, even if the lambda throws.</p>
 */
public final class EmailDeliveryContext {

    private EmailDeliveryContext() {}

    /**
     * Sets the template-type and community-id MDC tags, runs {@code action},
     * then clears the tags regardless of outcome.
     *
     * @param templateType human-readable template label (e.g. "OTP", "MATCH_REMINDER")
     * @param communityId  community the email belongs to (may be null)
     * @param action       the lambda that calls {@code emailService.send()} or {@code sendAll()}
     */
    public static void withContext(String templateType, Long communityId, Runnable action) {
        try {
            if (templateType != null) {
                MDC.put(SmtpEmailService.MDC_TEMPLATE_TYPE, templateType);
            }
            if (communityId != null) {
                MDC.put(SmtpEmailService.MDC_COMMUNITY_ID, communityId.toString());
            }
            action.run();
        } finally {
            MDC.remove(SmtpEmailService.MDC_TEMPLATE_TYPE);
            MDC.remove(SmtpEmailService.MDC_COMMUNITY_ID);
        }
    }

    /** Overload for cases where community context is not known. */
    public static void withContext(String templateType, Runnable action) {
        withContext(templateType, null, action);
    }
}
