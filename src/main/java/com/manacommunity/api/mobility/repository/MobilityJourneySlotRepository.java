package com.manacommunity.api.mobility.repository;

import com.manacommunity.api.mobility.model.MobilityJourneySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MobilityJourneySlotRepository extends JpaRepository<MobilityJourneySlot, Long> {
    List<MobilityJourneySlot> findByCommunityIdAndStatusAndJourneyTypeAndHubCodeOrderByDepartureTimeAsc(
            Long communityId, String status, String journeyType, String hubCode);
    List<MobilityJourneySlot> findByCommunityIdAndStatusOrderByDepartureTimeAsc(Long communityId, String status);
}
