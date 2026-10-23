package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventActivityRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventActivityRegistrationRepository extends JpaRepository<EventActivityRegistration, Long> {

    @Modifying
    @Query("DELETE FROM EventActivityRegistration r WHERE r.program.id IN (SELECT p.id FROM EventProgram p WHERE p.event.id = :eventId)")
    void deleteByProgramEventId(@Param("eventId") Long eventId);

    List<EventActivityRegistration> findByProgramIdOrderByRegisteredAtDesc(Long programId);

    List<EventActivityRegistration> findByUserIdOrderByRegisteredAtDesc(Long userId);

    Optional<EventActivityRegistration> findByProgramIdAndUserId(Long programId, Long userId);

    boolean existsByProgramIdAndUserId(Long programId, Long userId);

    Optional<EventActivityRegistration> findByProgramIdAndUserIdAndIdempotencyKey(Long programId, Long userId, String idempotencyKey);

    int countByProgramIdAndStatus(Long programId, EventActivityRegistration.ActivityRegStatus status);

    @Query("""
            SELECT COALESCE(SUM(r.headCount), 0)
            FROM EventActivityRegistration r
            WHERE r.program.id = :programId
              AND r.status = :status
            """)
    int sumHeadCountByProgramIdAndStatus(
            @Param("programId") Long programId,
            @Param("status") EventActivityRegistration.ActivityRegStatus status);

    Optional<EventActivityRegistration> findFirstByProgramIdAndStatusOrderByWaitlistPositionAscRegisteredAtAsc(
            Long programId,
            EventActivityRegistration.ActivityRegStatus status);

    @Query("SELECT r FROM EventActivityRegistration r WHERE r.program.event.id = :eventId ORDER BY r.registeredAt DESC")
    List<EventActivityRegistration> findByProgramEventId(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(r) FROM EventActivityRegistration r WHERE r.program.event.id = :eventId AND r.status <> 'CANCELLED'")
    long countByProgramEventId(@Param("eventId") Long eventId);
}
