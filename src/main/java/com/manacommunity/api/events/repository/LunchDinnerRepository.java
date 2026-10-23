package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.dto.LunchDinnerDashboardView;
import com.manacommunity.api.events.entity.EventLunchDinner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LunchDinnerRepository extends JpaRepository<EventLunchDinner, Long> {

    List<EventLunchDinner> findByCommunityIdOrderByDateAscStartTimeAsc(Long communityId);

    List<EventLunchDinner> findByMainEventIdOrderByDateAscStartTimeAsc(Long mainEventId);

    /** Find the meal config for capacity checks — matches event + date + mealType case-insensitively. */
    @Query("""
           SELECT m FROM EventLunchDinner m
           WHERE m.mainEventId = :eventId
             AND m.date = :date
             AND UPPER(m.mealType) = UPPER(:mealType)
           """)
    Optional<EventLunchDinner> findByEventAndDateAndType(@Param("eventId") Long eventId,
                                                         @Param("date") LocalDate date,
                                                         @Param("mealType") String mealType);

    /**
     * Projection query for scheduled upcoming meals for a single main event,
     * filtered by a set of needsRegistration values.
     *
     * <ul>
     *   <li>Pass {@code List.of(true)}         — registration-required meals only</li>
     *   <li>Pass {@code List.of(false)}         — open/free meals only</li>
     *   <li>Pass {@code List.of(true, false)}   — all upcoming meals</li>
     * </ul>
     *
     * Results are ordered: registration-required first, then by date and start-time.
     */
    @Query("SELECT e.id AS id, " +
            "       e.name AS name, " +
            "       e.mealType AS mealType, " +
            "       e.date AS date, " +
            "       e.startTime AS startTime, " +
            "       e.endTime AS endTime, " +
            "       e.venue AS venue, " +
            "       e.targetPlates AS targetPlates, " +
            "       e.isFree AS isFree, " +
            "       e.fee AS fee, " +
            "       e.dietType AS dietType, " +
            "       e.needsRegistration AS needsRegistration " +
            "FROM EventLunchDinner e " +
            "WHERE e.mainEventId = :mainEventId " +
            "  AND e.needsRegistration IN :needsRegistrationValues " +
            "  AND (e.date >= CURRENT_DATE OR e.date IS NULL) " +
            "ORDER BY e.needsRegistration DESC, e.date ASC, e.startTime ASC")
    List<LunchDinnerDashboardView> findMealsForDashboard(
            @Param("mainEventId") Long mainEventId,
            @Param("needsRegistrationValues") Collection<Boolean> needsRegistrationValues);

    /**
     * Projection query for scheduled upcoming meals across multiple main events,
     * filtered by a set of needsRegistration values.
     *
     * <ul>
     *   <li>Pass {@code List.of(true)}         — registration-required meals only</li>
     *   <li>Pass {@code List.of(false)}         — open/free meals only</li>
     *   <li>Pass {@code List.of(true, false)}   — all upcoming meals</li>
     * </ul>
     */
    @Query("SELECT e.id AS id, " +
            "       e.name AS name, " +
            "       e.mealType AS mealType, " +
            "       e.date AS date, " +
            "       e.startTime AS startTime, " +
            "       e.endTime AS endTime, " +
            "       e.venue AS venue, " +
            "       e.targetPlates AS targetPlates, " +
            "       e.isFree AS isFree, " +
            "       e.fee AS fee, " +
            "       e.dietType AS dietType, " +
            "       e.needsRegistration AS needsRegistration " +
            "FROM EventLunchDinner e " +
            "WHERE e.mainEventId IN :mainEventIds " +
            "  AND e.needsRegistration IN :needsRegistrationValues " +
            "  AND (e.date >= CURRENT_DATE OR e.date IS NULL) " +
            "ORDER BY e.needsRegistration DESC, e.date ASC, e.startTime ASC")
    List<LunchDinnerDashboardView> findMealsForDashboardByMainEventIds(
            @Param("mainEventIds") Collection<Long> mainEventIds,
            @Param("needsRegistrationValues") Collection<Boolean> needsRegistrationValues);

    /**
     * Entity-based query for upcoming meals requiring registration for a main event.
     */
    @Query("SELECT e FROM EventLunchDinner e " +
            "WHERE e.mainEventId = :mainEventId " +
            "  AND e.needsRegistration = true " +
            "  AND (e.date >= CURRENT_DATE OR e.date IS NULL) " +
            "ORDER BY e.date ASC, e.startTime ASC")
    List<EventLunchDinner> findUpcomingMealsByMainEventId(@Param("mainEventId") Long mainEventId);

    /**
     * Entity-based query for upcoming meals requiring registration across multiple main events.
     */
    @Query("SELECT e FROM EventLunchDinner e " +
            "WHERE e.mainEventId IN :mainEventIds " +
            "  AND e.needsRegistration = true " +
            "  AND (e.date >= CURRENT_DATE OR e.date IS NULL) " +
            "ORDER BY e.date ASC, e.startTime ASC")
    List<EventLunchDinner> findUpcomingMealsByMainEventIds(@Param("mainEventIds") Collection<Long> mainEventIds);

    /**
     * Counts active scheduled meals grouped by main event id.
     */
    @Query("SELECT e.mainEventId AS eventId, COUNT(e) AS count FROM EventLunchDinner e " +
            "WHERE e.mainEventId IN :mainEventIds " +
            "  AND (e.date >= CURRENT_DATE OR e.date IS NULL) " +
            "GROUP BY e.mainEventId")
    List<com.manacommunity.api.events.dto.EventActivityCountView> countActiveMealsGroupedByMainEventIdIn(
            @Param("mainEventIds") Collection<Long> mainEventIds);
}

