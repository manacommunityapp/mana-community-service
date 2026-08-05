package com.manacommunity.api.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * Utility for stripping dangerous HTML from user-generated content.
 *
 * <p>All user-supplied free-text fields (ticket descriptions, notice bodies,
 * comment messages, etc.) should pass through one of these methods before being
 * persisted, preventing Stored XSS attacks regardless of how the frontend
 * renders the data.</p>
 *
 * <p>Uses <a href="https://jsoup.org/">Jsoup</a> safe-list filtering:
 * <ul>
 *   <li>{@link #sanitizePlainText(String)} — strips <em>all</em> HTML tags.
 *       Use for single-line fields like ticket subjects, notice titles.</li>
 *   <li>{@link #sanitizeRichText(String)} — allows a minimal set of safe
 *       formatting tags (bold, italic, lists, links without javascript:).
 *       Use for multi-line body/description fields.</li>
 * </ul>
 * </p>
 */
public final class HtmlSanitizer {

    /**
     * Safe-list for rich-text fields: allows basic formatting but strips
     * all script/event-handler/iframe/object elements and javascript: hrefs.
     */
    private static final Safelist BASIC_SAFE = Safelist.basic();

    private HtmlSanitizer() {
        // utility class — no instances
    }

    /**
     * Strips all HTML tags. Input is returned as plain text.
     * Use for subject lines, titles, names, or any single-value field.
     *
     * @param input raw user input (may be null)
     * @return sanitized plain text, or null if input is null
     */
    public static String sanitizePlainText(String input) {
        if (input == null) return null;
        return Jsoup.clean(input, Safelist.none());
    }

    /**
     * Allows basic safe formatting (b, i, em, strong, ul, ol, li, a[href]).
     * Strips script tags, event handlers (onclick, onerror, …), iframes, and
     * javascript: / data: URIs. Use for description / body / comment fields.
     *
     * @param input raw user input (may be null)
     * @return sanitized HTML, or null if input is null
     */
    public static String sanitizeRichText(String input) {
        if (input == null) return null;
        return Jsoup.clean(input, BASIC_SAFE);
    }
}
