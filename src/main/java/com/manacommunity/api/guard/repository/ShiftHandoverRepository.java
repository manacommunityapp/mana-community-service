package com.manacommunity.api.guard.repository;

import com.manacommunity.api.guard.entity.ShiftHandover;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ShiftHandoverRepository extends JpaRepository<ShiftHandover, Long> {
    List<ShiftHandover> findByCommunityIdAndShiftDate(Long communityId, LocalDate date);
    List<ShiftHandover> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
}
