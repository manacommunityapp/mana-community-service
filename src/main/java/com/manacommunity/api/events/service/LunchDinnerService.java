package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.LunchDinnerRegistrationSummaryResponse;
import com.manacommunity.api.events.dto.LunchDinnerSummaryResponse;
import com.manacommunity.api.events.entity.EventLunchDinner;
import java.util.List;

public interface LunchDinnerService {

    List<EventLunchDinner> getAllLunchDinners(Long communityId, Long mainEventId);

    List<LunchDinnerSummaryResponse> getAllLunchDinnerSummaries(Long communityId, Long mainEventId);

    List<LunchDinnerRegistrationSummaryResponse> getRegistrationsForMeal(Long mealId, Long communityId);

    EventLunchDinner getLunchDinnerById(Long id, Long communityId);

    EventLunchDinner createLunchDinner(Long communityId, EventLunchDinner lunchDinner);

    EventLunchDinner updateLunchDinner(Long id, Long communityId, EventLunchDinner lunchDinner);

    void deleteLunchDinner(Long id, Long communityId);
}
