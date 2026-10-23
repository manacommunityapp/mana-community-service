package com.manacommunity.api.homeservice.repository;

import com.manacommunity.api.homeservice.model.entity.HomeServiceAttendanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository("homeServiceAttendanceRepository")
public interface HomeServiceAttendanceRepository extends JpaRepository<HomeServiceAttendanceEntity, String> {
    List<HomeServiceAttendanceEntity> findByBookingIdOrderByServiceDateDesc(String bookingId);
    List<HomeServiceAttendanceEntity> findByWorkerIdAndServiceDateBetween(String workerId, LocalDate startDate, LocalDate endDate);
    List<HomeServiceAttendanceEntity> findByBookingIdAndServiceDateBetween(String bookingId, LocalDate startDate, LocalDate endDate);
}
