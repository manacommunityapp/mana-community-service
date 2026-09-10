package com.manacommunity.api.events.dto;

import com.manacommunity.api.events.enums.PoojaSevaStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Spring Data JPA projection for scheduled pooja/seva activities in the dashboard view.
 */
public interface PoojaScheduledActivityView {

    Long getId();

    String getName();

    String getType();

    LocalDate getDate();

    LocalDate getEndDate();

    LocalTime getStartTime();

    LocalTime getEndTime();

    Boolean getIsFree();

    BigDecimal getFee();

    Integer getSlots();

    Boolean getNeedsRegistration();

    PoojaSevaStatus getStatus();
}
