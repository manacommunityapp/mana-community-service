package com.manacommunity.api.communityops.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StaffShiftRepository extends JpaRepository<StaffShift, Long> {
    List<StaffShift> findByCommunityIdAndShiftDateBetween(Long communityId, LocalDate from, LocalDate to);
    Optional<StaffShift> findByCommunityIdAndStaffUserIdAndShiftDate(Long communityId, Long staffUserId, LocalDate date);
    long countByCommunityIdAndShiftDateAndStatus(Long communityId, LocalDate date, AttendanceStatus status);
    List<StaffShift> findByCommunityIdAndShiftDate(Long communityId, LocalDate date);
}
