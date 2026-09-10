package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketGroupOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketGroupOrderRepository extends JpaRepository<MarketGroupOrder, Long> {
    Page<MarketGroupOrder> findByCommunityIdAndStatus(Long communityId, MarketGroupOrder.GroupOrderStatus status, Pageable pageable);
    List<MarketGroupOrder> findByOrganizerIdOrderByCreatedAtDesc(Long organizerId);
    int countByCommunityIdAndStatus(Long communityId, MarketGroupOrder.GroupOrderStatus status);
}
