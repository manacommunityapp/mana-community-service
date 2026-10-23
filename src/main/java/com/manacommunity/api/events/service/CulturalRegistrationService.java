package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.EventCulturalRegistration;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface CulturalRegistrationService {

    EventCulturalRegistration createRegistration(EventCulturalRegistration request, AppUser caller, Long communityId, boolean adminOverride);

    List<EventCulturalRegistration> getMyRegistrations(AppUser user, Long communityId);

    List<EventCulturalRegistration> getRegistrationsByCommunity(Long communityId);

    List<EventCulturalRegistration> getRegistrationsByCulturalEvent(Long culturalEventId);

    List<EventCulturalRegistration> getRegistrationsByMainEvent(Long mainEventId);

    EventCulturalRegistration getById(Long id, AppUser caller);

    void cancelRegistration(Long id, String reason, AppUser caller);

    void deleteRegistration(Long id, AppUser caller);

    EventCulturalRegistration checkIn(Long id, AppUser caller);
}
