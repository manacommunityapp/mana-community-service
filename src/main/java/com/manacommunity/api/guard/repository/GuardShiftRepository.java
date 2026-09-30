package com.manacommunity.api.guard.repository;

import com.manacommunity.api.guard.entity.GuardShift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface GuardShiftRepository extends JpaRepository<GuardShift, Long> {

    List<GuardShift> findByCommunityIdAndShiftDateOrderByStartTimeAsc(Long communityId, LocalDate shiftDate);

    List<GuardShift> findByCommunityIdAndShiftDateBetweenOrderByShiftDateAscStartTimeAsc(
            Long communityId, LocalDate from, LocalDate to);

    List<GuardShift> findByGuardIdOrderByShiftDateDescStartTimeDesc(Long guardId);

    List<GuardShift> findByGuardIdAndShiftDate(Long guardId, LocalDate shiftDate);

    long countByCommunityIdAndShiftDate(Long communityId, LocalDate shiftDate);
}
