package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.EventPoojaSeva;
import java.util.List;

public interface PoojaSevaService {

    List<EventPoojaSeva> getAllPoojaSevas(Long communityId, Long mainEventId);

    EventPoojaSeva getPoojaSevaById(Long id, Long communityId);

    EventPoojaSeva createPoojaSeva(Long communityId, EventPoojaSeva poojaSeva);

    EventPoojaSeva updatePoojaSeva(Long id, Long communityId, EventPoojaSeva poojaSeva);

    void deletePoojaSeva(Long id, Long communityId);
}
