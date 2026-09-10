package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.dto.PoojaScheduledActivityView;
import com.manacommunity.api.events.entity.EventPoojaSeva;
import com.manacommunity.api.events.enums.PoojaSevaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PoojaSevaRepository extends JpaRepository<EventPoojaSeva, Long> {

    List<EventPoojaSeva> findByCommunityIdOrderByDateAscStartTimeAsc(Long communityId);

    List<EventPoojaSeva> findByMainEventIdOrderByDateAscStartTimeAsc(Long mainEventId);

    List<EventPoojaSeva> findByCommunityIdAndMainEventIdOrderByDateAscStartTimeAsc(Long communityId, Long mainEventId);

    Optional<EventPoojaSeva> findByIdAndCommunityId(Long id, Long communityId);

    /**
     * Counts active Pooja and Seva items for a specific main event.
     */
    @Query("SELECT COUNT(e) FROM EventPoojaSeva e WHERE e.status = com.manacommunity.api.events.enums.PoojaSevaStatus.ACTIVE AND e.mainEventId = :mainEventId")
    long countActiveByMainEventId(@Param("mainEventId") Long mainEventId);

    /**
     * Counts active Pooja and Seva items for a collection of main events.
     */
    @Query("SELECT COUNT(e) FROM EventPoojaSeva e WHERE e.status = com.manacommunity.api.events.enums.PoojaSevaStatus.ACTIVE AND e.mainEventId IN :mainEventIds")
    long countActiveByMainEventIdIn(@Param("mainEventIds") Collection<Long> mainEventIds);

    /**
     * Counts active Pooja and Seva items grouped by main event id.
     */
    @Query("SELECT e.mainEventId AS eventId, COUNT(e) AS count FROM EventPoojaSeva e " +
            "WHERE e.status = com.manacommunity.api.events.enums.PoojaSevaStatus.ACTIVE " +
            "AND e.mainEventId IN :mainEventIds GROUP BY e.mainEventId")
    List<com.manacommunity.api.events.dto.EventActivityCountView> countActiveGroupedByMainEventIdIn(
            @Param("mainEventIds") Collection<Long> mainEventIds);

    /**
     * Derived query method to count by mainEventId and status.
     */
    long countByMainEventIdAndStatus(Long mainEventId, PoojaSevaStatus status);

    /**
     * Derived query method to count by mainEventIds and status.
     */
    long countByMainEventIdInAndStatus(Collection<Long> mainEventIds, PoojaSevaStatus status);

    /**
     * Projection query for scheduled active pooja/seva activities for a single main event,
     * filtered by a set of needsRegistration values.
     *
     * <ul>
     *   <li>Pass {@code List.of(true)}         — registration-required sevas only</li>
     *   <li>Pass {@code List.of(false)}         — open-to-all sevas only</li>
     *   <li>Pass {@code List.of(true, false)}   — all active sevas</li>
     * </ul>
     *
     * Results are ordered: registration-required first, then by date and start-time.
     */
    @Query("SELECT e.id AS id, " +
            "       e.name AS name, " +
            "       e.type AS type, " +
            "       e.date AS date, " +
            "       e.endDate AS endDate, " +
            "       e.startTime AS startTime, " +
            "       e.endTime AS endTime, " +
            "       e.isFree AS isFree, " +
            "       e.fee AS fee, " +
            "       e.slots AS slots, " +
            "       e.needsRegistration AS needsRegistration, " +
            "       e.status AS status " +
            "FROM EventPoojaSeva e " +
            "WHERE e.status = com.manacommunity.api.events.enums.PoojaSevaStatus.ACTIVE " +
            "  AND e.mainEventId = :mainEventId " +
            "  AND e.needsRegistration IN :needsRegistrationValues " +
            "ORDER BY e.needsRegistration DESC, e.date ASC, e.startTime ASC")
    List<PoojaScheduledActivityView> findScheduledActivitiesForDashboard(
            @Param("mainEventId") Long mainEventId,
            @Param("needsRegistrationValues") Collection<Boolean> needsRegistrationValues);

    /**
     * Projection query for scheduled active pooja/seva activities across multiple main events,
     * filtered by a set of needsRegistration values.
     *
     * <ul>
     *   <li>Pass {@code List.of(true)}         — registration-required sevas only</li>
     *   <li>Pass {@code List.of(false)}         — open-to-all sevas only</li>
     *   <li>Pass {@code List.of(true, false)}   — all active sevas</li>
     * </ul>
     */
    @Query("SELECT e.id AS id, " +
            "       e.name AS name, " +
            "       e.type AS type, " +
            "       e.date AS date, " +
            "       e.endDate AS endDate, " +
            "       e.startTime AS startTime, " +
            "       e.endTime AS endTime, " +
            "       e.isFree AS isFree, " +
            "       e.fee AS fee, " +
            "       e.slots AS slots, " +
            "       e.needsRegistration AS needsRegistration, " +
            "       e.status AS status " +
            "FROM EventPoojaSeva e " +
            "WHERE e.status = com.manacommunity.api.events.enums.PoojaSevaStatus.ACTIVE " +
            "  AND e.mainEventId IN :mainEventIds " +
            "  AND e.needsRegistration IN :needsRegistrationValues " +
            "ORDER BY e.needsRegistration DESC, e.date ASC, e.startTime ASC")
    List<PoojaScheduledActivityView> findScheduledActivitiesForDashboardByMainEventIds(
            @Param("mainEventIds") Collection<Long> mainEventIds,
            @Param("needsRegistrationValues") Collection<Boolean> needsRegistrationValues);

    /**
     * Entity-based query method to fetch active scheduled activities requiring registration.
     */
    List<EventPoojaSeva> findByMainEventIdAndStatusAndNeedsRegistrationTrueOrderByDateAscStartTimeAsc(
            Long mainEventId, PoojaSevaStatus status);

    /**
     * Entity-based query method to fetch active scheduled activities requiring registration across multiple events.
     */
    List<EventPoojaSeva> findByMainEventIdInAndStatusAndNeedsRegistrationTrueOrderByDateAscStartTimeAsc(
            Collection<Long> mainEventIds, PoojaSevaStatus status);
}

