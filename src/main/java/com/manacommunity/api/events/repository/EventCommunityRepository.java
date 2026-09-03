package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.dto.EventDashboardView;
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

    @Query("SELECT e.id AS id, " +
            "       e.category AS category, " +
            "       e.capacity AS capacity, " +
            "       e.description AS description, " +
            "       e.startDate AS startDate, " +
            "       e.endDate AS endDate, " +
            "       e.imageUrl AS imageUrl, " +
            "       e.registrationDeadline AS registrationDeadline, " +
            "       e.location AS location, " +
            "       e.city AS city, " +
            "       e.title AS title, " +
            "       e.type AS type, " +
            "       e.venue AS venue, " +
            "       e.status AS status " +
            "FROM EventCommunity e WHERE e.community.id = :communityId " +
            "AND e.status IN (com.manacommunity.api.events.entity.EventCommunity.EventStatus.PUBLISHED, " +
            "                 com.manacommunity.api.events.entity.EventCommunity.EventStatus.ACTIVE) " +
            "AND (e.endDate >= CURRENT_DATE OR (e.endDate IS NULL AND e.startDate >= CURRENT_DATE)) " +
            "ORDER BY e.startDate ASC")
    List<EventDashboardView> findActiveAndPublishedEventsForDashboard(@Param("communityId") Long communityId);

    long countByCommunityId(Long communityId);

    @Query("SELECT COUNT(e) FROM EventCommunity e WHERE e.community.id = :communityId " +
            "AND e.status <> com.manacommunity.api.events.entity.EventCommunity.EventStatus.CANCELLED " +
            "AND (e.endDate >= CURRENT_DATE OR (e.endDate IS NULL AND e.startDate >= CURRENT_DATE))")
    long countUpcomingByCommunity(@Param("communityId") Long communityId);
}
