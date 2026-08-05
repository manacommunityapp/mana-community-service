package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventSponsor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventSponsorRepository extends JpaRepository<EventSponsor, Long> {

    List<EventSponsor> findByEventIdOrderByTierAscNameAsc(Long eventId);

    List<EventSponsor> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    @Query("SELECT COALESCE(SUM(s.amountReceived), 0) FROM EventSponsor s WHERE s.event.community.id = :communityId")
    double sumAmountReceivedByCommunity(@Param("communityId") Long communityId);
}
