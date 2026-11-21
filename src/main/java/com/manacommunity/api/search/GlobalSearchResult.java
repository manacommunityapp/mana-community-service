package com.manacommunity.api.search;

/**
 * A single cross-module search hit.
 *
 * @param type        Module/entity type: RESIDENT, MAINTENANCE, EVENT, NOTICE, VISITOR
 * @param id          Primary key of the matching record
 * @param title       Short display label (name, subject, title, pass code, …)
 * @param subtitle    Secondary context (flat, status, category, …)
 * @param communityId Community the record belongs to
 */
public record GlobalSearchResult(
        String type,
        Long   id,
        String title,
        String subtitle,
        Long   communityId
) {}
