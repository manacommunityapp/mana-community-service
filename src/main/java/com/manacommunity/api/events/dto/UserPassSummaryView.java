package com.manacommunity.api.events.dto;

/**
 * Spring Data JPA projection for pass and devotee counts across activity categories.
 */
public interface UserPassSummaryView {

    Long getTotalPasses();

    Long getPoojaPasses();

    Long getMealPasses();

    Long getCulturalPasses();

    Long getGeneralPasses();
}
