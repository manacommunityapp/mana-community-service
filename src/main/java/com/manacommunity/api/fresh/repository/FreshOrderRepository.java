package com.manacommunity.api.fresh.repository;

import com.manacommunity.api.fresh.entity.FreshOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FreshOrderRepository extends JpaRepository<FreshOrder, Long> {
    List<FreshOrder> findByResidentIdOrderByCreatedAtDesc(Long residentId);
    List<FreshOrder> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
}
