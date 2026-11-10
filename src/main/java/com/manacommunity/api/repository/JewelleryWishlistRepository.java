package com.manacommunity.api.repository;

import com.manacommunity.api.model.JewelleryProduct;
import com.manacommunity.api.model.JewelleryWishlist;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JewelleryWishlistRepository extends JpaRepository<JewelleryWishlist, Long> {

    List<JewelleryWishlist> findByUserOrderBySavedAtDesc(AppUser user);

    Optional<JewelleryWishlist> findByUserAndProduct(AppUser user, JewelleryProduct product);

    boolean existsByUserAndProduct(AppUser user, JewelleryProduct product);

    void deleteByUserAndProduct(AppUser user, JewelleryProduct product);
}
