package com.manacommunity.api.repository;

import com.manacommunity.api.model.JewelleryVendorProfile;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JewelleryVendorProfileRepository extends JpaRepository<JewelleryVendorProfile, Long> {

    Optional<JewelleryVendorProfile> findByUser(AppUser user);

    List<JewelleryVendorProfile> findByIsActiveTrueOrderByRatingDesc();

    Optional<JewelleryVendorProfile> findByIdAndIsActiveTrue(Long id);

    boolean existsByUser(AppUser user);
}
