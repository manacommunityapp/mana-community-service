package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketWishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketWishlistRepository extends JpaRepository<MarketWishlist, Long> {
    List<MarketWishlist> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<MarketWishlist> findByUserIdAndListingId(Long userId, Long listingId);
    boolean existsByUserIdAndListingId(Long userId, Long listingId);
    void deleteByUserIdAndListingId(Long userId, Long listingId);
}
