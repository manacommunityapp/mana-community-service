package com.manacommunity.api.events.dto;

import com.manacommunity.api.events.entity.EventCulturalEvent;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Spring Data JPA projection for scheduled cultural activities in the dashboard view.
 */
public interface CulturalScheduledActivityView {

    Long getId();

    String getName();

    String getCategory();

    String getPerfType();

    String getAgeGroup();

    LocalDate getDate();

    LocalTime getStartTime();

    Integer getDuration();

    String getStage();

    Boolean getNeedsRegistration();

    Integer getCapacity();

    LocalDate getRegDeadline();

    Integer getSortOrder();

    EventCulturalEvent.CulturalEventStatus getStatus();
}
