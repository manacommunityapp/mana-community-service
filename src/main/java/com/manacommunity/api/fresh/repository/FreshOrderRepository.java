package com.manacommunity.api.fresh.repository;

import com.manacommunity.api.fresh.entity.FreshOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FreshOrderRepository extends JpaRepository<FreshOrder, Long> {
    List<FreshOrder> findByResidentIdOrderByCreatedAtDesc(Long residentId);
    List<FreshOrder> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    /** Owner-scoped single-order lookup — use instead of findById in controllers. */
    Optional<FreshOrder> findByIdAndResidentId(Long id, Long residentId);

    /** Tenant-scoped single-order lookup (community admins). */
    Optional<FreshOrder> findByIdAndCommunityId(Long id, Long communityId);
}
