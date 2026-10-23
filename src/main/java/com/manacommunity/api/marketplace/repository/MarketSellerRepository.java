package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketSellerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketSellerRepository extends JpaRepository<MarketSellerProfile, Long> {

    Optional<MarketSellerProfile> findByUserId(Long userId);

    Page<MarketSellerProfile> findByCommunityIdAndIsActiveTrue(Long communityId, Pageable pageable);

    List<MarketSellerProfile> findByKycStatus(MarketSellerProfile.KycStatus kycStatus);

    Page<MarketSellerProfile> findByCommunityIdAndKycStatus(Long communityId, MarketSellerProfile.KycStatus kycStatus, Pageable pageable);
}
