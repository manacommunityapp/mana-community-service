package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventDonation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventDonationRepository extends JpaRepository<EventDonation, Long> {

    List<EventDonation> findByEventIdOrderByCreatedAtDesc(Long eventId);

    List<EventDonation> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM EventDonation d WHERE d.event.id = :eventId")
    double sumAmountByEvent(@Param("eventId") Long eventId);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM EventDonation d WHERE d.community.id = :communityId")
    double sumAmountByCommunity(@Param("communityId") Long communityId);
}
