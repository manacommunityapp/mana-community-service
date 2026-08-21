package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.CommunityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommunityEventRepository extends JpaRepository<CommunityEvent, Long> {

    List<CommunityEvent> findByCommunityIdOrderByStartDateDesc(Long communityId);

    List<CommunityEvent> findByCreatedByIdOrderByCreatedAtDesc(Long userId);

    Optional<CommunityEvent> findByIdAndCommunity_Id(Long id, Long communityId);

    @Query("SELECT e FROM CommunityEvent e WHERE e.community.id = :communityId " +
            "AND e.status <> com.manacommunity.api.events.entity.CommunityEvent$EventStatus.CANCELLED " +
            "AND (e.endDate >= CURRENT_DATE OR (e.endDate IS NULL AND e.startDate >= CURRENT_DATE)) " +
            "ORDER BY e.startDate ASC")
    List<CommunityEvent> findUpcomingByCommunity(@Param("communityId") Long communityId);

    @Query("SELECT e FROM CommunityEvent e WHERE e.community.id = :communityId AND e.type = :type " +
            "AND e.status <> com.manacommunity.api.events.entity.CommunityEvent$EventStatus.CANCELLED " +
            "AND (e.endDate >= CURRENT_DATE OR (e.endDate IS NULL AND e.startDate >= CURRENT_DATE)) " +
            "ORDER BY e.startDate ASC")
    List<CommunityEvent> findUpcomingByCommunityAndType(
            @Param("communityId") Long communityId,
            @Param("type") CommunityEvent.EventType type);

    long countByCommunityId(Long communityId);

    @Query("SELECT COUNT(e) FROM CommunityEvent e WHERE e.community.id = :communityId " +
            "AND e.status <> com.manacommunity.api.events.entity.CommunityEvent$EventStatus.CANCELLED " +
            "AND (e.endDate >= CURRENT_DATE OR (e.endDate IS NULL AND e.startDate >= CURRENT_DATE))")
    long countUpcomingByCommunity(@Param("communityId") Long communityId);
}
