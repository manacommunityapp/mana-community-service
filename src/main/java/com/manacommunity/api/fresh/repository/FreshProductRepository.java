package com.manacommunity.api.fresh.repository;

import com.manacommunity.api.fresh.entity.FreshProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FreshProductRepository extends JpaRepository<FreshProduct, Long> {
    List<FreshProduct> findByCommunityIdAndActiveTrueOrderByNameAsc(Long communityId);
    List<FreshProduct> findByCommunityIdOrderByNameAsc(Long communityId);

    /** Tenant-scoped single-product lookup — use instead of findById in controllers. */
    Optional<FreshProduct> findByIdAndCommunityId(Long id, Long communityId);

    /** Tenant-scoped lookup restricted to orderable (active) products. */
    Optional<FreshProduct> findByIdAndCommunityIdAndActiveTrue(Long id, Long communityId);
}
