package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.EventPoojaUserRegistration;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface EventPoojaUserRegistrationService {

    EventPoojaUserRegistration createRegistration(EventPoojaUserRegistration registration, AppUser user, Long communityId);

    EventPoojaUserRegistration createRegistration(EventPoojaUserRegistration registration, AppUser user, Long communityId, boolean adminOverride);

    EventPoojaUserRegistration updateRegistration(Long id, EventPoojaUserRegistration patch, AppUser user);

    List<EventPoojaUserRegistration> getMyRegistrations(AppUser user, Long communityId);

    List<EventPoojaUserRegistration> getRegistrationsByCommunity(Long communityId);

    EventPoojaUserRegistration getRegistrationById(Long id, AppUser user);

    void cancelRegistration(Long id, AppUser user);

    void deleteRegistration(Long id, AppUser user);
}
