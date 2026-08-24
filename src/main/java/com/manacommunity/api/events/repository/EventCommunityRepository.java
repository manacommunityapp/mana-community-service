package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventCommunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventCommunityRepository extends JpaRepository<EventCommunity, Long> {

    List<EventCommunity> findByCommunityIdOrderByStartDateDesc(Long communityId);

    List<EventCommunity> findByCreatedByIdOrderByCreatedAtDesc(Long userId);

    Optional<EventCommunity> findByIdAndCommunity_Id(Long id, Long communityId);

    @Query("SELECT e FROM EventCommunity e WHERE e.community.id = :communityId " +
            "AND e.status <> com.manacommunity.api.events.entity.EventCommunity.EventStatus.CANCELLED " +
            "AND (e.endDate >= CURRENT_DATE OR (e.endDate IS NULL AND e.startDate >= CURRENT_DATE)) " +
            "ORDER BY e.startDate ASC")
    List<EventCommunity> findUpcomingByCommunity(@Param("communityId") Long communityId);

    @Query("SELECT e FROM EventCommunity e WHERE e.community.id = :communityId AND e.type = :type " +
            "AND e.status <> com.manacommunity.api.events.entity.EventCommunity.EventStatus.CANCELLED " +
            "AND (e.endDate >= CURRENT_DATE OR (e.endDate IS NULL AND e.startDate >= CURRENT_DATE)) " +
            "ORDER BY e.startDate ASC")
    List<EventCommunity> findUpcomingByCommunityAndType(
            @Param("communityId") Long communityId,
            @Param("type") EventCommunity.EventType type);

    long countByCommunityId(Long communityId);

    @Query("SELECT COUNT(e) FROM EventCommunity e WHERE e.community.id = :communityId " +
            "AND e.status <> com.manacommunity.api.events.entity.EventCommunity.EventStatus.CANCELLED " +
            "AND (e.endDate >= CURRENT_DATE OR (e.endDate IS NULL AND e.startDate >= CURRENT_DATE))")
    long countUpcomingByCommunity(@Param("communityId") Long communityId);
}
