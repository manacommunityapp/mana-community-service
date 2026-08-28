package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventPoojaUserRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventPoojaUserRegistrationRepository extends JpaRepository<EventPoojaUserRegistration, Long> {

    List<EventPoojaUserRegistration> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<EventPoojaUserRegistration> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<EventPoojaUserRegistration> findByEventIdOrderByCreatedAtDesc(Long eventId);

    Optional<EventPoojaUserRegistration> findByRegCode(String regCode);

    boolean existsByRegCode(String regCode);

    Optional<EventPoojaUserRegistration> findByIdAndUserId(Long id, Long userId);

    long countByEventIdAndStatusNot(Long eventId, String status);

    boolean existsByUserIdAndEventIdAndPoojaSlotDateAndStatusNot(Long userId, Long eventId, String slotDate, String status);

    /** Count confirmed (non-cancelled) registrations for a given schedule. */
    @Query("""
           SELECT COUNT(r) FROM EventPoojaUserRegistration r
           WHERE r.scheduleId = :scheduleId AND r.status NOT IN ('CANCELLED')
           """)
    long countConfirmedByScheduleId(@Param("scheduleId") Long scheduleId);

    /** Sum of devoteeCount for non-cancelled registrations against a schedule. */
    @Query("""
           SELECT COALESCE(SUM(r.devoteeCount), 0) FROM EventPoojaUserRegistration r
           WHERE r.scheduleId = :scheduleId AND r.status NOT IN ('CANCELLED')
           """)
    int sumDevoteeCountByScheduleId(@Param("scheduleId") Long scheduleId);

    /**
     * M-3: Count non-cancelled registrations for a schedule that have NO reservation
     * (admin-direct adds, legacy imports).  These are invisible to the reservation-based
     * availability calc and must be subtracted separately.
     */
    @Query("""
           SELECT COUNT(r) FROM EventPoojaUserRegistration r
           WHERE r.scheduleId = :scheduleId
             AND r.reservationId IS NULL
             AND r.status NOT IN ('CANCELLED')
           """)
    long countDirectRegistrationsByScheduleId(@Param("scheduleId") Long scheduleId);

    /** M-3: Sum of devoteeCount for direct (no-reservation) registrations against a schedule. */
    @Query("""
           SELECT COALESCE(SUM(r.devoteeCount), 0) FROM EventPoojaUserRegistration r
           WHERE r.scheduleId = :scheduleId
             AND r.reservationId IS NULL
             AND r.status NOT IN ('CANCELLED')
           """)
    int sumDirectDevoteesByScheduleId(@Param("scheduleId") Long scheduleId);

    /**
     * G-4: Schedule-level duplicate guard — returns true when the user already has a
     * non-cancelled registration for the given schedule row.
     * Used alongside the pessimistic-lock reservation path to catch admin-direct bookings
     * that bypass the reservation flow.
     */
    boolean existsByUserIdAndScheduleIdAndStatusNot(Long userId, Long scheduleId, String status);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE EventPoojaUserRegistration r SET r.scheduleId = :targetScheduleId WHERE r.scheduleId = :sourceScheduleId")
    int migrateScheduleId(@Param("sourceScheduleId") Long sourceScheduleId, @Param("targetScheduleId") Long targetScheduleId);

    List<EventPoojaUserRegistration> findByPoojaSevaIdOrderByCreatedAtDesc(Long poojaSevaId);

    List<EventPoojaUserRegistration> findByCommunityIdAndPoojaSevaIdOrderByCreatedAtDesc(Long communityId, Long poojaSevaId);
}
