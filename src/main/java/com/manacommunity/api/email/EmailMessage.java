package com.manacommunity.api.email;

import lombok.Builder;

/**
 * A single, fully-rendered HTML email ready to be dispatched.
 *
 * @param to        recipient address
 * @param toName    recipient display name (may be null)
 * @param subject   email subject line
 * @param htmlBody  rendered HTML body
 * @param from      sender address override (may be null)
 * @param fromName  sender display name override (may be null)
 */
@Builder
public record EmailMessage(String to, String toName, String subject, String htmlBody, String from, String fromName) {
    public EmailMessage(String to, String toName, String subject, String htmlBody) {
        this(to, toName, subject, htmlBody, null, null);
    }
}

