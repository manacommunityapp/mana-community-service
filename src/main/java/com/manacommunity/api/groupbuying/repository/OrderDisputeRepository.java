package com.manacommunity.api.groupbuying.repository;

import com.manacommunity.api.groupbuying.model.OrderDispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderDisputeRepository extends JpaRepository<OrderDispute, Long> {
    List<OrderDispute> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<OrderDispute> findByDisputeNumber(String disputeNumber);
    Optional<OrderDispute> findByOrderId(String orderId);
}
