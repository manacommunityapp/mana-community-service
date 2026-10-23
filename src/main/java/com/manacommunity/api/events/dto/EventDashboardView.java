package com.manacommunity.api.events.dto;

import com.manacommunity.api.events.entity.EventCommunity;

import java.time.LocalDate;

/**
 * Spring Data JPA projection for the active and published events dashboard list.
 */
public interface EventDashboardView {

    Long getId();

    String getTitle();

    String getDescription();

    EventCommunity.EventType getType();

    LocalDate getStartDate();

    LocalDate getEndDate();

    String getLocation();

    String getVenue();

    String getCity();

    String getCategory();

    Integer getCapacity();

    String getImageUrl();

    LocalDate getRegistrationDeadline();

    EventCommunity.EventStatus getStatus();
}
