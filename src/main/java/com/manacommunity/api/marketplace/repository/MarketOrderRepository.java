package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketOrderRepository extends JpaRepository<MarketOrder, Long> {

    Optional<MarketOrder> findByOrderNumber(String orderNumber);

    Page<MarketOrder> findByBuyerId(Long buyerId, Pageable pageable);

    Page<MarketOrder> findBySellerId(Long sellerId, Pageable pageable);

    Page<MarketOrder> findByCommunityId(Long communityId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM MarketOrder o WHERE o.seller.id = :sellerId AND o.status = 'COMPLETED'")
    BigDecimal calculateSellerGmv(@Param("sellerId") Long sellerId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM MarketOrder o WHERE o.community.id = :communityId AND o.status = 'COMPLETED'")
    BigDecimal calculateCommunityGmv(@Param("communityId") Long communityId);

    int countBySellerId(Long sellerId);

    int countByCommunityId(Long communityId);
}
