package com.manacommunity.api.repository;

import com.manacommunity.api.model.PlatformShowcaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformShowcaseItemRepository extends JpaRepository<PlatformShowcaseItem, Long> {
    List<PlatformShowcaseItem> findByActiveTrueOrderBySortOrderAscIdAsc();
    Optional<PlatformShowcaseItem> findByItemKey(String itemKey);
}
