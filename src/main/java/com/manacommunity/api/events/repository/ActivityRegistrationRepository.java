package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.ActivityRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActivityRegistrationRepository extends JpaRepository<ActivityRegistration, Long> {

    @Modifying
    @Query("DELETE FROM ActivityRegistration r WHERE r.program.id IN (SELECT p.id FROM EventProgram p WHERE p.event.id = :eventId)")
    void deleteByProgramEventId(@Param("eventId") Long eventId);

    List<ActivityRegistration> findByProgramIdOrderByRegisteredAtDesc(Long programId);

    List<ActivityRegistration> findByUserIdOrderByRegisteredAtDesc(Long userId);

    Optional<ActivityRegistration> findByProgramIdAndUserId(Long programId, Long userId);

    boolean existsByProgramIdAndUserId(Long programId, Long userId);

    Optional<ActivityRegistration> findByProgramIdAndUserIdAndIdempotencyKey(Long programId, Long userId, String idempotencyKey);

    int countByProgramIdAndStatus(Long programId, ActivityRegistration.ActivityRegStatus status);

    @Query("""
            SELECT COALESCE(SUM(r.headCount), 0)
            FROM ActivityRegistration r
            WHERE r.program.id = :programId
              AND r.status = :status
            """)
    int sumHeadCountByProgramIdAndStatus(
            @Param("programId") Long programId,
            @Param("status") ActivityRegistration.ActivityRegStatus status);

    Optional<ActivityRegistration> findFirstByProgramIdAndStatusOrderByWaitlistPositionAscRegisteredAtAsc(
            Long programId,
            ActivityRegistration.ActivityRegStatus status);
}
