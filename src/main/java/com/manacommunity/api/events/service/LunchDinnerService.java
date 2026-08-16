package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.LunchDinner;
import java.util.List;

public interface LunchDinnerService {

    List<LunchDinner> getAllLunchDinners(Long communityId, Long mainEventId);

    LunchDinner getLunchDinnerById(Long id, Long communityId);

    LunchDinner createLunchDinner(Long communityId, LunchDinner lunchDinner);

    LunchDinner updateLunchDinner(Long id, Long communityId, LunchDinner lunchDinner);

    void deleteLunchDinner(Long id, Long communityId);
}
