package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceChannel;
import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.model.CommerceOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommerceOrderRepository extends JpaRepository<CommerceOrder, Long> {
    Optional<CommerceOrder> findByOrderNumber(String orderNumber);
    Optional<CommerceOrder> findByQrToken(String qrToken);
    List<CommerceOrder> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    List<CommerceOrder> findByCommunityIdAndChannelOrderByCreatedAtDesc(Long communityId, CommerceChannel channel);
    List<CommerceOrder> findByVendorIdOrderByCreatedAtDesc(String vendorId);
    List<CommerceOrder> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    List<CommerceOrder> findByCommunityIdAndStatus(Long communityId, CommerceOrderStatus status);

    // Phase 4 additions
    List<CommerceOrder> findByVendorIdAndStatusOrderByCreatedAtDesc(String vendorId, CommerceOrderStatus status);
    List<CommerceOrder> findBySellerIdAndStatusOrderByCreatedAtDesc(Long sellerId, CommerceOrderStatus status);

    @Query("SELECT o FROM CommerceOrder o WHERE o.community.id = :communityId ORDER BY o.createdAt DESC")
    Page<CommerceOrder> findByCommunityId(Long communityId, Pageable pageable);

    @Query("SELECT o FROM CommerceOrder o WHERE o.community.id = :communityId AND o.status = :status ORDER BY o.createdAt DESC")
    Page<CommerceOrder> findByCommunityIdAndStatusPaged(Long communityId, CommerceOrderStatus status, Pageable pageable);

    long countByBuyerIdAndStatus(Long buyerId, CommerceOrderStatus status);

    // Phase 7: settlement cycle queries
    @Query("SELECT o FROM CommerceOrder o WHERE o.seller.id = :sellerId AND o.status = 'COMPLETED' AND o.createdAt BETWEEN :start AND :end")
    List<CommerceOrder> findCompletedOrdersForSellerInCycle(Long sellerId, Instant start, Instant end);

    @Query("SELECT o FROM CommerceOrder o WHERE o.vendorId = :vendorId AND o.status = 'COMPLETED' AND o.createdAt BETWEEN :start AND :end")
    List<CommerceOrder> findCompletedOrdersForVendorInCycle(String vendorId, Instant start, Instant end);

    // Phase 11: analytics aggregates
    @Query("SELECT COUNT(o) FROM CommerceOrder o WHERE o.community.id = :communityId AND o.createdAt BETWEEN :from AND :to")
    long countOrdersInPeriod(@Param("communityId") Long communityId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM CommerceOrder o WHERE o.community.id = :communityId AND o.status = 'COMPLETED' AND o.createdAt BETWEEN :from AND :to")
    BigDecimal sumRevenueInPeriod(@Param("communityId") Long communityId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(o) FROM CommerceOrder o WHERE o.community.id = :communityId AND o.status = 'COMPLETED' AND o.createdAt BETWEEN :from AND :to")
    long countCompletedInPeriod(@Param("communityId") Long communityId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(o) FROM CommerceOrder o WHERE o.community.id = :communityId AND o.status = 'CANCELLED' AND o.createdAt BETWEEN :from AND :to")
    long countCancelledInPeriod(@Param("communityId") Long communityId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT i.sku, i.title, SUM(i.quantity) AS qty FROM CommerceOrderItem i WHERE i.order.community.id = :communityId AND i.order.createdAt BETWEEN :from AND :to GROUP BY i.sku, i.title ORDER BY qty DESC")
    List<Object[]> findTopSkusByQuantity(@Param("communityId") Long communityId, @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}