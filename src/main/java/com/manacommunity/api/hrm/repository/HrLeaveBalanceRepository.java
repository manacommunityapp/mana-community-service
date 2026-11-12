package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrLeaveBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HrLeaveBalanceRepository extends JpaRepository<HrLeaveBalance, Long> {

    List<HrLeaveBalance> findByEmployeeIdAndYear(Long employeeId, Integer year);

    Optional<HrLeaveBalance> findByEmployeeIdAndLeaveTypeIdAndYear(Long employeeId, Long leaveTypeId, Integer year);

    List<HrLeaveBalance> findByLeaveTypeIdAndYear(Long leaveTypeId, Integer year);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM HrLeaveBalance b WHERE b.employee.id = :empId AND b.leaveType.id = :typeId AND b.year = :year")
    Optional<HrLeaveBalance> findByEmployeeAndTypeAndYearWithLock(
            @Param("empId") Long empId, @Param("typeId") Long typeId, @Param("year") Integer year);
}
