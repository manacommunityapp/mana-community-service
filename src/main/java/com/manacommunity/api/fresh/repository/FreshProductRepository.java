package com.manacommunity.api.fresh.repository;

import com.manacommunity.api.fresh.entity.FreshProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FreshProductRepository extends JpaRepository<FreshProduct, Long> {
    List<FreshProduct> findByCommunityIdAndActiveTrueOrderByNameAsc(Long communityId);
    List<FreshProduct> findByCommunityIdOrderByNameAsc(Long communityId);
}
