package com.manacommunity.api.repository;

import com.manacommunity.api.model.GroceryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroceryItemRepository extends JpaRepository<GroceryItem, Long> {

    List<GroceryItem> findByCommunityIdAndAvailableTrueOrderByNameAsc(Long communityId);

    List<GroceryItem> findByCommunityIdOrderByNameAsc(Long communityId);

    Optional<GroceryItem> findByIdAndCommunityId(Long id, Long communityId);
}
