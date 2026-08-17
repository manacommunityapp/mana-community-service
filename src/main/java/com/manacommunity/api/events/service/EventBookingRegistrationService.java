package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.EventBookingRegistration;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface EventBookingRegistrationService {

    EventBookingRegistration createRegistration(EventBookingRegistration registration, AppUser user, Long communityId);

    List<EventBookingRegistration> getMyRegistrations(AppUser user, Long communityId);

    List<EventBookingRegistration> getRegistrationsByCommunity(Long communityId);

    EventBookingRegistration getRegistrationById(Long id, AppUser user);

    void cancelRegistration(Long id, AppUser user);
}
