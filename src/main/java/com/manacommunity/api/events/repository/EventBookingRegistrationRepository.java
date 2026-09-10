package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.dto.UserPassSummaryView;
import com.manacommunity.api.events.entity.EventBookingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventBookingRegistrationRepository extends JpaRepository<EventBookingRegistration, Long> {

    List<EventBookingRegistration> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<EventBookingRegistration> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    List<EventBookingRegistration> findByUserIdAndStatusNotOrderByCreatedAtDesc(Long userId, String status);

    List<EventBookingRegistration> findByCommunityId(Long communityId);

    List<EventBookingRegistration> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<EventBookingRegistration> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, String status);

    List<EventBookingRegistration> findByCommunityIdAndStatusNotOrderByCreatedAtDesc(Long communityId, String status);

    Optional<EventBookingRegistration> findByRegCode(String regCode);

    Optional<EventBookingRegistration> findByIdAndUserId(Long id, Long userId);

    List<EventBookingRegistration> findByActivityId(String activityId);

    boolean existsByActivityIdAndStatusNot(String activityId, String status);

    long countByActivityIdAndStatusNot(String activityId, String status);

    List<EventBookingRegistration> findByActivityTitle(String activityTitle);

    long countByCommunityId(Long communityId);

    /** True when the user already has a non-cancelled booking for the given activityId. */
    boolean existsByUserIdAndActivityIdAndStatusNot(Long userId, String activityId, String status);

    boolean existsByUserIdAndActivityIdInAndStatusNot(Long userId, Collection<String> activityIds, String status);

    List<EventBookingRegistration> findByMainEventIdOrderByCreatedAtDesc(Long mainEventId);

    long countByMainEventIdAndStatusNot(Long mainEventId, String status);

    long countByUserId(Long userId);

    // ── Dashboard count queries (Quick Actions KPIs — no entity fetch needed) ───

    /**
     * Total non-cancelled registrations with a parent event — myRegistrationsCount.
     *
     * SELECT COUNT(b) FROM event_booking_registration b
     * WHERE b.user_id = :userId
     *   AND UPPER(b.status) <> 'CANCELLED'
     *   AND b.main_event_id IS NOT NULL
     */
    @Query("SELECT COUNT(b) FROM EventBookingRegistration b " +
           "WHERE b.user.id = :userId " +
           "AND UPPER(b.status) <> 'CANCELLED' " +
           "AND b.mainEventId IS NOT NULL")
    long countUserRegistrations(@Param("userId") Long userId);

    /**
     * Pooja / Seva registrations — myPoojaCount Quick Action badge.
     *
     * SELECT COUNT(*) FROM event_booking_registrations b
     * WHERE b.user_id = :userId
     *   AND UPPER(b.status) <> 'CANCELLED'
     *   AND b.main_event_id IS NOT NULL
     *   AND (LOWER(b.category) LIKE '%pooja%' OR LOWER(b.category) LIKE '%seva%')
     */
    @Query("SELECT COUNT(b) FROM EventBookingRegistration b " +
           "WHERE b.user.id = :userId " +
           "AND UPPER(b.status) <> 'CANCELLED' " +
           "AND b.mainEventId IS NOT NULL " +
           "AND (LOWER(b.category) LIKE '%pooja%' OR LOWER(b.category) LIKE '%seva%')")
    long countUserPoojaRegistrations(@Param("userId") Long userId);

    /**
     * Meal / Food registrations — myMealCount Quick Action badge.
     *
     * SELECT COUNT(*) FROM event_booking_registrations b
     * WHERE b.user_id = :userId
     *   AND UPPER(b.status) <> 'CANCELLED'
     *   AND b.main_event_id IS NOT NULL
     *   AND (LOWER(b.category) LIKE '%meal%' OR LOWER(b.category) LIKE '%food%'
     *        OR LOWER(b.category) LIKE '%lunch%' OR LOWER(b.category) LIKE '%dinner%')
     */
    @Query("SELECT COUNT(b) FROM EventBookingRegistration b " +
           "WHERE b.user.id = :userId " +
           "AND UPPER(b.status) <> 'CANCELLED' " +
           "AND b.mainEventId IS NOT NULL " +
           "AND (LOWER(b.category) LIKE '%meal%' OR LOWER(b.category) LIKE '%food%' " +
           "     OR LOWER(b.category) LIKE '%lunch%' OR LOWER(b.category) LIKE '%dinner%')")
    long countUserMealRegistrations(@Param("userId") Long userId);

    /**
     * Cultural registrations — myCulturalCount Quick Action badge.
     *
     * SELECT COUNT(*) FROM event_booking_registrations b
     * WHERE b.user_id = :userId
     *   AND UPPER(b.status) <> 'CANCELLED'
     *   AND b.main_event_id IS NOT NULL
     *   AND (LOWER(b.category) LIKE '%cultural%' OR LOWER(b.category) LIKE '%cult%')
     */
    @Query("SELECT COUNT(b) FROM EventBookingRegistration b " +
           "WHERE b.user.id = :userId " +
           "AND UPPER(b.status) <> 'CANCELLED' " +
           "AND b.mainEventId IS NOT NULL " +
           "AND (LOWER(b.category) LIKE '%cultural%' OR LOWER(b.category) LIKE '%cult%')")
    long countUserCulturalRegistrations(@Param("userId") Long userId);

    /**
     * Registration list with event start date in one JOIN — fixes the N+1 inside
     * EventUserDashboardService.getDashboard() where eventRepo.findById() was called
     * once per registration.
     *
     * Uses nativeQuery=true because mainEventId is a plain Long column (no @ManyToOne
     * mapping to EventCommunity), so JPQL JOIN ... ON is not supported.
     *
     * Returns Object[]: [0]=id  [1]=main_event_id  [2]=activity_title
     *                   [3]=category  [4]=status  [5]=created_at  [6]=start_date
     *
     * SELECT b.id, b.main_event_id, b.activity_title, b.category, b.status,
     *        b.created_at, e.start_date
     * FROM event_booking_registration b
     * LEFT JOIN event_community e ON e.id = b.main_event_id
     * WHERE b.user_id = :userId
     *   AND UPPER(b.status) <> 'CANCELLED'
     *   AND b.main_event_id IS NOT NULL
     * ORDER BY b.created_at DESC
     */
    @Query(value =
           "SELECT b.id, b.main_event_id, b.activity_title, b.category, b.status, " +
           "       b.created_at, e.start_date " +
           "FROM event_booking_registrations b " +
           "LEFT JOIN event_community e ON e.id = b.main_event_id " +
           "WHERE b.user_id = :userId " +
           "  AND UPPER(b.status) <> 'CANCELLED' " +
           "  AND b.main_event_id IS NOT NULL " +
           "ORDER BY b.created_at DESC",
           nativeQuery = true)
    List<Object[]> findUserRegProjectionsWithEventDate(@Param("userId") Long userId);

    /**
     * Pass and devotee count breakdown across all categories for a user within a community
     * where event status is PUBLISHED or ACTIVE, not expired, and registration is not cancelled.
     */
    @Query(value =
           "SELECT " +
           "    COALESCE(SUM(eb.devotee_count), 0) AS totalPasses, " +
           "    COALESCE(SUM(CASE WHEN LOWER(eb.category) LIKE '%pooja%' OR LOWER(eb.category) LIKE '%seva%' " +
           "                      THEN eb.devotee_count ELSE 0 END), 0) AS poojaPasses, " +
           "    COALESCE(SUM(CASE WHEN LOWER(eb.category) LIKE '%meal%' OR LOWER(eb.category) LIKE '%food%' " +
           "                           OR LOWER(eb.category) LIKE '%lunch%' OR LOWER(eb.category) LIKE '%dinner%' " +
           "                      THEN eb.devotee_count ELSE 0 END), 0) AS mealPasses, " +
           "    COALESCE(SUM(CASE WHEN LOWER(eb.category) LIKE '%cultural%' OR LOWER(eb.category) LIKE '%cult%' " +
           "                      THEN eb.devotee_count ELSE 0 END), 0) AS culturalPasses, " +
           "    COALESCE(SUM(CASE WHEN LOWER(eb.category) NOT LIKE '%pooja%' " +
           "                           AND LOWER(eb.category) NOT LIKE '%seva%' " +
           "                           AND LOWER(eb.category) NOT LIKE '%meal%' " +
           "                           AND LOWER(eb.category) NOT LIKE '%food%' " +
           "                           AND LOWER(eb.category) NOT LIKE '%lunch%' " +
           "                           AND LOWER(eb.category) NOT LIKE '%dinner%' " +
           "                           AND LOWER(eb.category) NOT LIKE '%cultural%' " +
           "                           AND LOWER(eb.category) NOT LIKE '%cult%' " +
           "                      THEN eb.devotee_count ELSE 0 END), 0) AS generalPasses " +
           "FROM event_booking_registrations eb " +
           "JOIN event_community ec ON ec.id = eb.main_event_id " +
           "WHERE eb.user_id = :userId " +
           "  AND UPPER(eb.status) <> 'CANCELLED' " +
           "  AND UPPER(eb.status) <> 'REJECTED' " +
           "  AND ec.community_id = :communityId " +
           "  AND ec.status IN ('PUBLISHED', 'ACTIVE') " +
           "  AND (ec.end_date >= CURRENT_DATE OR (ec.end_date IS NULL AND ec.start_date >= CURRENT_DATE))",
           nativeQuery = true)
    UserPassSummaryView countActiveUserPassSummaryByCommunity(
            @Param("userId") Long userId,
            @Param("communityId") Long communityId);

    /**
     * Pass and devotee count breakdown scoped to a specific main event for a user.
     */
    @Query(value =
           "SELECT " +
           "    COALESCE(SUM(eb.devotee_count), 0) AS totalPasses, " +
           "    COALESCE(SUM(CASE WHEN LOWER(eb.category) LIKE '%pooja%' OR LOWER(eb.category) LIKE '%seva%' " +
           "                      THEN eb.devotee_count ELSE 0 END), 0) AS poojaPasses, " +
           "    COALESCE(SUM(CASE WHEN LOWER(eb.category) LIKE '%meal%' OR LOWER(eb.category) LIKE '%food%' " +
           "                           OR LOWER(eb.category) LIKE '%lunch%' OR LOWER(eb.category) LIKE '%dinner%' " +
           "                      THEN eb.devotee_count ELSE 0 END), 0) AS mealPasses, " +
           "    COALESCE(SUM(CASE WHEN LOWER(eb.category) LIKE '%cultural%' OR LOWER(eb.category) LIKE '%cult%' " +
           "                      THEN eb.devotee_count ELSE 0 END), 0) AS culturalPasses, " +
           "    COALESCE(SUM(CASE WHEN LOWER(eb.category) NOT LIKE '%pooja%' " +
           "                           AND LOWER(eb.category) NOT LIKE '%seva%' " +
           "                           AND LOWER(eb.category) NOT LIKE '%meal%' " +
           "                           AND LOWER(eb.category) NOT LIKE '%food%' " +
           "                           AND LOWER(eb.category) NOT LIKE '%lunch%' " +
           "                           AND LOWER(eb.category) NOT LIKE '%dinner%' " +
           "                           AND LOWER(eb.category) NOT LIKE '%cultural%' " +
           "                           AND LOWER(eb.category) NOT LIKE '%cult%' " +
           "                      THEN eb.devotee_count ELSE 0 END), 0) AS generalPasses " +
           "FROM event_booking_registrations eb " +
           "JOIN event_community ec ON ec.id = eb.main_event_id " +
           "WHERE eb.user_id = :userId " +
           "  AND eb.main_event_id = :mainEventId " +
           "  AND UPPER(eb.status) <> 'CANCELLED' " +
           "  AND UPPER(eb.status) <> 'REJECTED' " +
           "  AND ec.community_id = :communityId " +
           "  AND ec.status IN ('PUBLISHED', 'ACTIVE') " +
           "  AND (ec.end_date >= CURRENT_DATE OR (ec.end_date IS NULL AND ec.start_date >= CURRENT_DATE))",
           nativeQuery = true)
    UserPassSummaryView countActiveUserPassSummaryByCommunityAndEvent(
            @Param("userId") Long userId,
            @Param("communityId") Long communityId,
            @Param("mainEventId") Long mainEventId);

    /**
     * Pass and devotee count breakdown across the 4 booking tables for a user
     * scoped directly to the provided active event IDs using a clean UNION ALL pattern.
     */
    @Query(value =
           "SELECT " +
           "    COALESCE(SUM(passes), 0) AS totalPasses, " +
           "    COALESCE(SUM(CASE WHEN type = 'POOJA' THEN passes ELSE 0 END), 0) AS poojaPasses, " +
           "    COALESCE(SUM(CASE WHEN type = 'MEAL' THEN passes ELSE 0 END), 0) AS mealPasses, " +
           "    COALESCE(SUM(CASE WHEN type = 'CULTURAL' THEN passes ELSE 0 END), 0) AS culturalPasses, " +
           "    COALESCE(SUM(CASE WHEN type = 'GENERAL' THEN passes ELSE 0 END), 0) AS generalPasses " +
           "FROM ( " +
           "    SELECT devotee_count AS passes, 'GENERAL' AS type " +
           "    FROM event_booking_registrations " +
           "    WHERE user_id = :userId " +
           "      AND main_event_id IN (:eventIds) " +
           "      AND UPPER(status) NOT IN ('CANCELLED', 'REJECTED') " +
           "    UNION ALL " +
           "    SELECT devotee_count AS passes, 'POOJA' AS type " +
           "    FROM event_pooja_user_registrations " +
           "    WHERE user_id = :userId " +
           "      AND event_id IN (:eventIds) " +
           "      AND UPPER(status) NOT IN ('CANCELLED', 'REJECTED') " +
           "    UNION ALL " +
           "    SELECT head_count AS passes, 'MEAL' AS type " +
           "    FROM event_meal_registrations " +
           "    WHERE user_id = :userId " +
           "      AND event_id IN (:eventIds) " +
           "    UNION ALL " +
           "    SELECT devotee_count AS passes, 'CULTURAL' AS type " +
           "    FROM event_cultural_registrations " +
           "    WHERE user_id = :userId " +
           "      AND main_event_id IN (:eventIds) " +
           "      AND UPPER(status) NOT IN ('CANCELLED', 'REJECTED') " +
           ") t",
           nativeQuery = true)
    UserPassSummaryView countPassSummaryByUserAndActiveEvents(
            @Param("userId") Long userId,
            @Param("eventIds") Collection<Long> eventIds);
}
