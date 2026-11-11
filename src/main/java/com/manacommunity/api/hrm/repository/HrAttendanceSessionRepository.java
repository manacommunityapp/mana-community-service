package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrAttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HrAttendanceSessionRepository extends JpaRepository<HrAttendanceSession, Long> {

    List<HrAttendanceSession> findByAttendanceIdOrderBySessionInAsc(Long attendanceId);

    Optional<HrAttendanceSession> findTopByAttendanceIdAndSessionOutIsNullOrderBySessionInDesc(Long attendanceId);
}
