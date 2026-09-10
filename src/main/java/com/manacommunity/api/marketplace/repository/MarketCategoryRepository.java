package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketListingCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketCategoryRepository extends JpaRepository<MarketListingCategory, Long> {
    List<MarketListingCategory> findByActiveTrueOrderBySortOrderAsc();
    List<MarketListingCategory> findByParentIdAndActiveTrueOrderBySortOrderAsc(Long parentId);
    Optional<MarketListingCategory> findBySlug(String slug);
}
