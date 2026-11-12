package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrAttendanceLog;
import com.manacommunity.api.hrm.enums.AttendanceEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HrAttendanceLogRepository extends JpaRepository<HrAttendanceLog, Long> {

    List<HrAttendanceLog> findByEmployeeIdAndEventTimeBetween(Long employeeId, LocalDateTime from, LocalDateTime to);

    Optional<HrAttendanceLog> findTopByEmployeeIdAndEventTypeOrderByEventTimeDesc(Long employeeId, AttendanceEventType eventType);
}
