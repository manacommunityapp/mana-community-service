package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrAttendanceCorrection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HrAttendanceCorrectionRepository extends JpaRepository<HrAttendanceCorrection, Long> {
    List<HrAttendanceCorrection> findByAttendanceId(Long attendanceId);
}
