package com.manacommunity.api.email;

/**
 * A single, fully-rendered HTML email ready to be dispatched.
 *
 * @param to        recipient address
 * @param toName    recipient display name (may be null)
 * @param subject   email subject line
 * @param htmlBody  rendered HTML body
 */
public record EmailMessage(String to, String toName, String subject, String htmlBody) {
}
