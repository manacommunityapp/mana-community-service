package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceDispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommerceDisputeRepository extends JpaRepository<CommerceDispute, Long> {
    Optional<CommerceDispute> findByDisputeCode(String disputeCode);
    List<CommerceDispute> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CommerceDispute> findByOrderId(Long orderId);
}