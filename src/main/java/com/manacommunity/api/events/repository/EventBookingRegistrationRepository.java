package com.manacommunity.api.events.repository;

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
}

