package com.manacommunity.api.emergency.repository;

import com.manacommunity.api.emergency.entity.EvacuationMusterRoster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvacuationMusterRosterRepository extends JpaRepository<EvacuationMusterRoster, Long> {
    List<EvacuationMusterRoster> findByBroadcastIdOrderByPersonCategoryAscFlatNumberAsc(Long broadcastId);
}
