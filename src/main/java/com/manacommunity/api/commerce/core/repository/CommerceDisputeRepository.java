package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceDispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommerceDisputeRepository extends JpaRepository<CommerceDispute, Long> {
    Optional<CommerceDispute> findByDisputeCode(String disputeCode);
    List<CommerceDispute> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CommerceDispute> findByOrderId(Long orderId);

    @Query("SELECT d FROM CommerceDispute d WHERE d.order.community.id = :communityId ORDER BY d.createdAt DESC")
    Page<CommerceDispute> findByCommunityIdPaged(@Param("communityId") Long communityId, Pageable pageable);

    @Query("SELECT d FROM CommerceDispute d WHERE d.order.community.id = :communityId AND d.status = :status ORDER BY d.createdAt DESC")
    Page<CommerceDispute> findByCommunityIdAndStatusPaged(@Param("communityId") Long communityId, @Param("status") String status, Pageable pageable);
}