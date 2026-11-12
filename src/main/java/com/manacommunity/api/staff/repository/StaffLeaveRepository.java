package com.manacommunity.api.staff.repository;

import com.manacommunity.api.staff.entity.StaffLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StaffLeaveRepository extends JpaRepository<StaffLeave, Long> {
    @Query("SELECT l FROM StaffLeave l WHERE l.staffId = :staffId AND FUNCTION('TO_CHAR', l.leaveDate, 'YYYY-MM') = :month ORDER BY l.leaveDate")
    List<StaffLeave> findByStaffIdAndMonth(@Param("staffId") Long staffId, @Param("month") String month);

    List<StaffLeave> findByStaffIdAndLeaveDateBetweenOrderByLeaveDate(Long staffId, LocalDate from, LocalDate to);
}
