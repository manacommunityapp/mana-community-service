package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventPoojaSlotReservation;
import com.manacommunity.api.events.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventPoojaSlotReservationRepository extends JpaRepository<EventPoojaSlotReservation, Long> {

    /**
     * Expires all RESERVED rows for a given schedule whose hold has timed out.
     * Must be called inside the same PESSIMISTIC_WRITE transaction on the schedule row.
     * Uses native SQL so the enum string value is unambiguous.
     */
    @Modifying
    @Query(value = """
           UPDATE event_pooja_slot_reservation
           SET status = 'EXPIRED', updated_at = NOW()
           WHERE schedule_id = :scheduleId
             AND status = 'RESERVED'
             AND expires_at < :now
           """, nativeQuery = true)
    int expireStaleForSchedule(@Param("scheduleId") Long scheduleId, @Param("now") LocalDateTime now);

    /** Sum of family slots currently held (RESERVED, not yet expired) for a schedule. */
    @Query("""
           SELECT COALESCE(SUM(r.reservedFamilyCount), 0) FROM EventPoojaSlotReservation r
           WHERE r.schedule.id = :scheduleId
             AND r.status = 'RESERVED'
             AND r.expiresAt >= :now
           """)
    int sumActiveReservedFamilies(@Param("scheduleId") Long scheduleId, @Param("now") LocalDateTime now);

    /** Sum of devotee slots currently held for a schedule. */
    @Query("""
           SELECT COALESCE(SUM(r.reservedDevoteeCount), 0) FROM EventPoojaSlotReservation r
           WHERE r.schedule.id = :scheduleId
             AND r.status = 'RESERVED'
             AND r.expiresAt >= :now
           """)
    int sumActiveReservedDevotees(@Param("scheduleId") Long scheduleId, @Param("now") LocalDateTime now);

    /** Sum of confirmed (registration linked) family slots for a schedule. */
    @Query("""
           SELECT COALESCE(SUM(r.reservedFamilyCount), 0) FROM EventPoojaSlotReservation r
           WHERE r.schedule.id = :scheduleId
             AND r.status = 'CONFIRMED'
           """)
    int sumConfirmedFamilies(@Param("scheduleId") Long scheduleId);

    /** Sum of confirmed devotee slots for a schedule. */
    @Query("""
           SELECT COALESCE(SUM(r.reservedDevoteeCount), 0) FROM EventPoojaSlotReservation r
           WHERE r.schedule.id = :scheduleId
             AND r.status = 'CONFIRMED'
           """)
    int sumConfirmedDevotees(@Param("scheduleId") Long scheduleId);

    /** Used by the expiry scheduler to bulk-expire rows system-wide. */
    @Modifying
    @Query(value = """
           UPDATE event_pooja_slot_reservation
           SET status = 'EXPIRED', updated_at = NOW()
           WHERE status = 'RESERVED' AND expires_at < :now
           """, nativeQuery = true)
    int expireAllStale(@Param("now") LocalDateTime now);

    /** Find all globally expired but not-yet-swept rows (for auditing/logging). */
    @Query("""
           SELECT r FROM EventPoojaSlotReservation r
           WHERE r.status = 'RESERVED' AND r.expiresAt < :now
           """)
    List<EventPoojaSlotReservation> findExpiredReservations(@Param("now") LocalDateTime now);

    /** Check whether a user already has an active (RESERVED or CONFIRMED) reservation for a schedule. */
    @Query("""
           SELECT r FROM EventPoojaSlotReservation r
           WHERE r.schedule.id = :scheduleId
             AND r.user.id = :userId
             AND r.status IN ('RESERVED','CONFIRMED')
           """)
    Optional<EventPoojaSlotReservation> findActiveByScheduleAndUser(
            @Param("scheduleId") Long scheduleId, @Param("userId") Long userId);

    Optional<EventPoojaSlotReservation> findByIdempotencyKey(String key);

    /** M-2: Scope idempotency lookup to the specific schedule so a retried key can't lock out a different slot. */
    @Query("""
           SELECT r FROM EventPoojaSlotReservation r
           WHERE r.idempotencyKey = :key AND r.schedule.id = :scheduleId
           """)
    Optional<EventPoojaSlotReservation> findByIdempotencyKeyAndScheduleId(
            @Param("key") String key, @Param("scheduleId") Long scheduleId);

    Optional<EventPoojaSlotReservation> findByRegistrationId(Long registrationId);

    List<EventPoojaSlotReservation> findByScheduleIdOrderByCreatedAtDesc(Long scheduleId);

    /** #23: Purge EXPIRED / CANCELLED rows older than a given cutoff to keep the table lean. */
    @Modifying
    @Query("DELETE FROM EventPoojaSlotReservation r WHERE r.status IN ('EXPIRED','CANCELLED') AND r.updatedAt < :before")
    int deleteExpiredOrCancelledBefore(@Param("before") LocalDateTime before);
}
