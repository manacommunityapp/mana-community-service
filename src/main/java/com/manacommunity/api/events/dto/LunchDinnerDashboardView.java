package com.manacommunity.api.events.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Spring Data JPA projection for scheduled lunch/dinner/meal activities in the dashboard view.
 */
public interface LunchDinnerDashboardView {

    Long getId();

    String getName();

    String getMealType();

    LocalDate getDate();

    LocalTime getStartTime();

    LocalTime getEndTime();

    String getVenue();

    Integer getTargetPlates();

    Boolean getIsFree();

    BigDecimal getFee();

    String getDietType();

    Boolean getNeedsRegistration();
}
