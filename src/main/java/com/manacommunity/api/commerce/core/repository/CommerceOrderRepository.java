package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceChannel;
import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.model.CommerceOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}