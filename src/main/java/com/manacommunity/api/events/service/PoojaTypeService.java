package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.EventPoojaType;

import java.util.List;

public interface PoojaTypeService {

    List<EventPoojaType> getAllPoojaTypes(Long communityId);

    EventPoojaType createPoojaType(Long communityId, String name, String description);
}
