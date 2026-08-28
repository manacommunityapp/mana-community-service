package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventPoojaSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventPoojaScheduleRepository extends JpaRepository<EventPoojaSchedule, Long> {

    /**
     * Acquires a PESSIMISTIC_WRITE (SELECT ... FOR UPDATE) lock on the row.
     * All capacity checks and reservation creation must happen inside the same
     * transaction after calling this method.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM EventPoojaSchedule s WHERE s.id = :id")
    Optional<EventPoojaSchedule> findByIdForUpdate(@Param("id") Long id);

    List<EventPoojaSchedule> findByPoojaSeva_IdOrderByScheduleDateAscStartTimeAsc(Long poojaId);

    List<EventPoojaSchedule> findByPoojaSeva_IdAndScheduleDateOrderByStartTimeAsc(Long poojaId, LocalDate date);

    Optional<EventPoojaSchedule> findByPoojaSeva_IdAndScheduleDateAndStartTime(
            Long poojaId, LocalDate date, LocalTime startTime);

    Optional<EventPoojaSchedule> findByTimeSlotConfigId(Long timeSlotConfigId);

    /** Dates that have at least one non-BLOCKED, non-CLOSED slot. */
    @Query("""
           SELECT DISTINCT s.scheduleDate FROM EventPoojaSchedule s
           WHERE s.poojaSeva.id = :poojaId
             AND s.status NOT IN (
               com.manacommunity.api.events.enums.PoojaScheduleStatus.BLOCKED,
               com.manacommunity.api.events.enums.PoojaScheduleStatus.CLOSED)
           ORDER BY s.scheduleDate ASC
           """)
    List<LocalDate> findAvailableDatesByPoojaId(@Param("poojaId") Long poojaId);

    /** Atomically increments the token sequence and returns the PREVIOUS value (the token to assign). */
    @Modifying
    @Query("UPDATE EventPoojaSchedule s SET s.nextTokenSeq = s.nextTokenSeq + 1 WHERE s.id = :id")
    void incrementTokenSeq(@Param("id") Long id);

    @Query("SELECT s.nextTokenSeq FROM EventPoojaSchedule s WHERE s.id = :id")
    int getCurrentTokenSeq(@Param("id") Long id);
}
