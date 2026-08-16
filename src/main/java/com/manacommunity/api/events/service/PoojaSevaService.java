package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.PoojaSeva;
import java.util.List;

public interface PoojaSevaService {

    List<PoojaSeva> getAllPoojaSevas(Long communityId, Long mainEventId);

    PoojaSeva getPoojaSevaById(Long id, Long communityId);

    PoojaSeva createPoojaSeva(Long communityId, PoojaSeva poojaSeva);

    PoojaSeva updatePoojaSeva(Long id, Long communityId, PoojaSeva poojaSeva);

    void deletePoojaSeva(Long id, Long communityId);
}
