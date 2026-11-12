package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyVendorPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KittyPartyVendorPackageRepository extends JpaRepository<KittyPartyVendorPackage, Long> {

    List<KittyPartyVendorPackage> findByCommunityIdAndIsActiveTrueOrderByRatingDesc(Long communityId);

    List<KittyPartyVendorPackage> findByCommunityIdAndCategoryAndIsActiveTrueOrderByRatingDesc(Long communityId, String category);

    boolean existsByCommunityId(Long communityId);
}
