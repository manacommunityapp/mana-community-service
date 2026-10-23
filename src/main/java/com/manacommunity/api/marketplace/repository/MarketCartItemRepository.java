package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketCartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarketCartItemRepository extends JpaRepository<MarketCartItem, Long> {
    Optional<MarketCartItem> findByCartIdAndListingId(Long cartId, Long listingId);
    void deleteByCartId(Long cartId);
}
