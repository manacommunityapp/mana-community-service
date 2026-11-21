package com.manacommunity.api.search;

/**
 * A single cross-module search hit.
 *
 * @param type     Module/entity type: RESIDENT, MAINTENANCE, EVENT, NOTICE, VISITOR
 * @param id       Primary key of the matching record
 * @param title    Short display label (name, subject, title, pass code, …)
 * @param subtitle Secondary context (flat, status, category, …)
 * @param url      Relative frontend route for the hit (e.g. {@code /notices/12}).
 *                 Both the web and the mobile client read this field to navigate;
 *                 the previous {@code communityId} component left every result
 *                 unclickable. The value is always a server-built, server-controlled
 *                 path: it starts with a single '/', carries no scheme and no
 *                 protocol-relative "//" prefix, and interpolates only the numeric
 *                 primary key — never user-supplied text.
 */
public record GlobalSearchResult(
        String type,
        Long   id,
        String title,
        String subtitle,
        String url
) {}
