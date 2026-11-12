package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrAttendance;
import com.manacommunity.api.hrm.enums.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HrAttendanceRepository extends JpaRepository<HrAttendance, Long> {

    List<HrAttendance> findByEmployeeIdAndAttendanceDateBetween(Long employeeId, LocalDate from, LocalDate to);

    Optional<HrAttendance> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate date);

    Page<HrAttendance> findByOrganizationIdAndAttendanceDate(Long organizationId, LocalDate date, Pageable pageable);

    long countByOrganizationIdAndAttendanceDateAndAttendanceStatus(Long organizationId, LocalDate date, AttendanceStatus status);

    @Query("SELECT COUNT(a) > 0 FROM HrAttendance a WHERE a.employee.id = :empId AND a.attendanceDate = :date")
    boolean existsByEmployeeIdAndAttendanceDate(@Param("empId") Long empId, @Param("date") LocalDate date);

    List<HrAttendance> findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
            Long employeeId, LocalDate from, LocalDate to);
}
