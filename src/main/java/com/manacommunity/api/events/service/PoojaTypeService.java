package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.PoojaType;

import java.util.List;

public interface PoojaTypeService {

    List<PoojaType> getAllPoojaTypes(Long communityId);

    PoojaType createPoojaType(Long communityId, String name, String description);
}
