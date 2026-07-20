package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketplaceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarketplaceCategoryRepository extends JpaRepository<MarketplaceCategory, Long> {

    List<MarketplaceCategory> findByActiveOrderBySortOrderAsc(boolean active);

    List<MarketplaceCategory> findByCommunityIdAndActiveOrderBySortOrderAsc(Long communityId, boolean active);

    List<MarketplaceCategory> findByParentIdAndActive(Long parentId, boolean active);

    Optional<MarketplaceCategory> findBySlug(String slug);
}
