package com.manacommunity.api.retail.repository;

import com.manacommunity.api.retail.entity.RetailOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RetailOrderRepository extends JpaRepository<RetailOrder, Long> {

    List<RetailOrder> findByCommunityIdAndOrderTypeOrderByOrderDateDesc(
            Long communityId, RetailOrder.OrderType orderType);

    List<RetailOrder> findByCommunityIdOrderByOrderDateDesc(Long communityId);

    long countByCommunityIdAndOrderType(Long communityId, RetailOrder.OrderType orderType);
}
