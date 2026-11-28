package com.manacommunity.api.guardops.repository;

import com.manacommunity.api.guardops.entity.ShiftRoster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShiftRosterRepository extends JpaRepository<ShiftRoster, Long> {
    List<ShiftRoster> findByShiftDateAndCommunityId(LocalDate date, String communityId);
    List<ShiftRoster> findByGuardIdOrderByShiftDateDesc(Long guardId);
}
