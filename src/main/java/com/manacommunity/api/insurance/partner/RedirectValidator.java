package com.manacommunity.api.insurance.partner;

import java.net.URI;
import java.util.Locale;

/** Prevents open redirects: handoff URLs must be https, credential-free, and on the partner's own host. */
public final class RedirectValidator {
    private RedirectValidator() {}

    /** @param allowedHost the host registered for the partner (from handoff_base_url) */
    public static boolean isSafeHandoff(String url, String allowedHost) {
        if (url == null || allowedHost == null || url.isBlank() || url.length() > 2000) return false;
        try {
            URI u = new URI(url);
            if (!"https".equalsIgnoreCase(u.getScheme())) return false;
            if (u.getUserInfo() != null) return false;
            String host = u.getHost();
            if (host == null) return false;
            if (url.contains("\\") || url.chars().anyMatch(c -> c < 0x20)) return false;
            return host.toLowerCase(Locale.ROOT).equals(allowedHost.toLowerCase(Locale.ROOT));
        } catch (Exception e) {
            return false;
        }
    }

    /** Extracts host of a configured https base URL, or null when it is not a valid https URL. */
    public static String hostOf(String baseUrl) {
        try {
            URI u = new URI(baseUrl);
            if (!"https".equalsIgnoreCase(u.getScheme()) || u.getUserInfo() != null) return null;
            return u.getHost();
        } catch (Exception e) {
            return null;
        }
    }
}
