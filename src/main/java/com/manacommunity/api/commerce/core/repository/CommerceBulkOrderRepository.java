package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceBulkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommerceBulkOrderRepository extends JpaRepository<CommerceBulkOrder, Long> {
    Optional<CommerceBulkOrder> findByBulkOrderCode(String code);
    List<CommerceBulkOrder> findByCustomerUserIdOrderByCreatedAtDesc(Long customerId);
    List<CommerceBulkOrder> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
}
