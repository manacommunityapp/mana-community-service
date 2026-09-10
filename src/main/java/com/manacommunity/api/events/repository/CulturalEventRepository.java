package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.dto.CulturalScheduledActivityView;
import com.manacommunity.api.events.entity.EventCulturalEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CulturalEventRepository extends JpaRepository<EventCulturalEvent, Long> {

    List<EventCulturalEvent> findByCommunityIdOrderByDateAscStartTimeAscSortOrderAsc(Long communityId);

    List<EventCulturalEvent> findByMainEventIdOrderByDateAscStartTimeAscSortOrderAsc(Long mainEventId);

    /**
     * Counts active (non-cancelled) cultural events for a specific main event.
     */
    @Query("SELECT COUNT(e) FROM EventCulturalEvent e " +
            "WHERE e.mainEventId = :mainEventId " +
            "  AND e.status <> com.manacommunity.api.events.entity.EventCulturalEvent.CulturalEventStatus.CANCELLED")
    long countActiveByMainEventId(@Param("mainEventId") Long mainEventId);

    /**
     * Counts active (non-cancelled) cultural events for multiple main events.
     */
    @Query("SELECT COUNT(e) FROM EventCulturalEvent e " +
            "WHERE e.mainEventId IN :mainEventIds " +
            "  AND e.status <> com.manacommunity.api.events.entity.EventCulturalEvent.CulturalEventStatus.CANCELLED")
    long countActiveByMainEventIdIn(@Param("mainEventIds") Collection<Long> mainEventIds);

    /**
     * Counts active cultural events grouped by main event id.
     */
    @Query("SELECT e.mainEventId AS eventId, COUNT(e) AS count FROM EventCulturalEvent e " +
            "WHERE e.mainEventId IN :mainEventIds " +
            "  AND e.status <> com.manacommunity.api.events.entity.EventCulturalEvent.CulturalEventStatus.CANCELLED " +
            "GROUP BY e.mainEventId")
    List<com.manacommunity.api.events.dto.EventActivityCountView> countActiveGroupedByMainEventIdIn(
            @Param("mainEventIds") Collection<Long> mainEventIds);

    /**
     * Projection query for scheduled upcoming cultural activities for a single main event,
     * filtered by a set of needsRegistration values.
     *
     * <ul>
     *   <li>Pass {@code List.of(true)}         — registration-required performances only</li>
     *   <li>Pass {@code List.of(false)}         — open/free performances only</li>
     *   <li>Pass {@code List.of(true, false)}   — all active cultural activities</li>
     * </ul>
     *
     * Results are ordered: registration-required first, then by date, start-time and sort order.
     */
    @Query("SELECT e.id AS id, " +
            "       e.name AS name, " +
            "       e.category AS category, " +
            "       e.perfType AS perfType, " +
            "       e.ageGroup AS ageGroup, " +
            "       e.date AS date, " +
            "       e.startTime AS startTime, " +
            "       e.duration AS duration, " +
            "       e.stage AS stage, " +
            "       e.needsRegistration AS needsRegistration, " +
            "       e.capacity AS capacity, " +
            "       e.regDeadline AS regDeadline, " +
            "       e.sortOrder AS sortOrder, " +
            "       e.status AS status " +
            "FROM EventCulturalEvent e " +
            "WHERE e.mainEventId = :mainEventId " +
            "  AND e.needsRegistration IN :needsRegistrationValues " +
            "  AND e.status <> com.manacommunity.api.events.entity.EventCulturalEvent.CulturalEventStatus.CANCELLED " +
            "  AND (e.date >= CURRENT_DATE OR e.date IS NULL) " +
            "ORDER BY e.needsRegistration DESC, e.date ASC, e.startTime ASC, e.sortOrder ASC")
    List<CulturalScheduledActivityView> findCulturalEventsForDashboard(
            @Param("mainEventId") Long mainEventId,
            @Param("needsRegistrationValues") Collection<Boolean> needsRegistrationValues);

    /**
     * Projection query for scheduled upcoming cultural activities across multiple main events,
     * filtered by a set of needsRegistration values.
     *
     * <ul>
     *   <li>Pass {@code List.of(true)}         — registration-required performances only</li>
     *   <li>Pass {@code List.of(false)}         — open/free performances only</li>
     *   <li>Pass {@code List.of(true, false)}   — all active cultural activities</li>
     * </ul>
     */
    @Query("SELECT e.id AS id, " +
            "       e.name AS name, " +
            "       e.category AS category, " +
            "       e.perfType AS perfType, " +
            "       e.ageGroup AS ageGroup, " +
            "       e.date AS date, " +
            "       e.startTime AS startTime, " +
            "       e.duration AS duration, " +
            "       e.stage AS stage, " +
            "       e.needsRegistration AS needsRegistration, " +
            "       e.capacity AS capacity, " +
            "       e.regDeadline AS regDeadline, " +
            "       e.sortOrder AS sortOrder, " +
            "       e.status AS status " +
            "FROM EventCulturalEvent e " +
            "WHERE e.mainEventId IN :mainEventIds " +
            "  AND e.needsRegistration IN :needsRegistrationValues " +
            "  AND e.status <> com.manacommunity.api.events.entity.EventCulturalEvent.CulturalEventStatus.CANCELLED " +
            "  AND (e.date >= CURRENT_DATE OR e.date IS NULL) " +
            "ORDER BY e.needsRegistration DESC, e.date ASC, e.startTime ASC, e.sortOrder ASC")
    List<CulturalScheduledActivityView> findCulturalEventsForDashboardByMainEventIds(
            @Param("mainEventIds") Collection<Long> mainEventIds,
            @Param("needsRegistrationValues") Collection<Boolean> needsRegistrationValues);

    /**
     * Entity-based query for scheduled upcoming cultural activities requiring registration.
     */
    @Query("SELECT e FROM EventCulturalEvent e " +
            "WHERE e.mainEventId = :mainEventId " +
            "  AND e.needsRegistration = true " +
            "  AND e.status <> com.manacommunity.api.events.entity.EventCulturalEvent.CulturalEventStatus.CANCELLED " +
            "  AND (e.date >= CURRENT_DATE OR e.date IS NULL) " +
            "ORDER BY e.date ASC, e.startTime ASC, e.sortOrder ASC")
    List<EventCulturalEvent> findUpcomingCulturalEventsByMainEventId(@Param("mainEventId") Long mainEventId);
}
