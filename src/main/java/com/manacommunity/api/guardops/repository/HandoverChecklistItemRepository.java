package com.manacommunity.api.guardops.repository;

import com.manacommunity.api.guardops.entity.HandoverChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HandoverChecklistItemRepository extends JpaRepository<HandoverChecklistItem, Long> {
    List<HandoverChecklistItem> findByHandoverIdOrderByPriorityAsc(Long handoverId);
}
