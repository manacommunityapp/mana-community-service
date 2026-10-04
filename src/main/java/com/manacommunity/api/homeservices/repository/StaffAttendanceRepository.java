package com.manacommunity.api.homeservices.repository;

import com.manacommunity.api.homeservices.model.StaffAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, Long> {

    List<StaffAttendance> findByStaffCommunityIdAndDateOrderByStaffNameAsc(Long communityId, LocalDate date);

    List<StaffAttendance> findByStaffIdAndDateBetweenOrderByDateAsc(Long staffId, LocalDate from, LocalDate to);

    List<StaffAttendance> findByStaffCommunityIdAndDateBetweenOrderByStaffNameAsc(Long communityId, LocalDate from, LocalDate to);
}
