package com.manacommunity.api.staffpayroll.repository;

import com.manacommunity.api.staffpayroll.entity.AttendancePunch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendancePunchRepository extends JpaRepository<AttendancePunch, Long> {
    List<AttendancePunch> findByStaffIdAndPunchDateBetween(Long staffId, LocalDate start, LocalDate end);

    @Query("SELECT a FROM AttendancePunch a WHERE a.staffId = :staffId AND a.punchDate = :date AND a.punchType = 'GATE_IN'")
    List<AttendancePunch> findGateInForDate(Long staffId, LocalDate date);

    @Query("SELECT a.punchDate, a.attendanceStatus FROM AttendancePunch a WHERE a.staffId = :staffId AND a.punchDate BETWEEN :start AND :end ORDER BY a.punchDate")
    List<Object[]> getAttendanceSummary(Long staffId, LocalDate start, LocalDate end);
}
