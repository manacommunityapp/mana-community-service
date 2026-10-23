package com.manacommunity.api.safety.repository;

import com.manacommunity.api.safety.model.AnprEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AnprEventRepository extends JpaRepository<AnprEvent, Long> {

    List<AnprEvent> findByCommunityIdOrderByTimestampDesc(Long communityId);

    List<AnprEvent> findByCommunityIdAndGateOrderByTimestampDesc(Long communityId, String gate);

    List<AnprEvent> findByCommunityIdAndDirectionOrderByTimestampDesc(Long communityId, AnprEvent.Direction direction);

    List<AnprEvent> findByCommunityIdAndGateAndDirectionOrderByTimestampDesc(Long communityId, String gate, AnprEvent.Direction direction);

    List<AnprEvent> findByCommunityIdAndTimestampBetween(Long communityId, LocalDateTime from, LocalDateTime to);
}
