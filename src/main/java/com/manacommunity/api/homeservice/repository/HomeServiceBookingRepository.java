package com.manacommunity.api.homeservice.repository;

import com.manacommunity.api.homeservice.model.entity.HomeServiceBookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalTime;
import java.util.List;

@Repository("homeServiceBookingRepository")
public interface HomeServiceBookingRepository extends JpaRepository<HomeServiceBookingEntity, String> {
    List<HomeServiceBookingEntity> findByResidentUserIdOrderByCreatedAtDesc(String residentUserId);
    List<HomeServiceBookingEntity> findByWorkerIdOrderByCreatedAtDesc(String workerId);
    List<HomeServiceBookingEntity> findByCommunityIdOrderByCreatedAtDesc(String communityId);

    @Query("SELECT b FROM HomeServiceBookingEntity b WHERE b.workerId = :workerId " +
           "AND b.status IN ('REQUESTED', 'CONFIRMED', 'IN_PROGRESS') " +
           "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<HomeServiceBookingEntity> findConflictingBookings(
            @Param("workerId") String workerId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}
