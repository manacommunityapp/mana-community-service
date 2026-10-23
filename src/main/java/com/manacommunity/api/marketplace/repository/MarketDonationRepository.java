package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketDonation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketDonationRepository extends JpaRepository<MarketDonation, Long> {
    Page<MarketDonation> findByCommunityIdAndStatus(Long communityId, MarketDonation.DonationStatus status, Pageable pageable);
    Page<MarketDonation> findByCommunityIdAndSharingModeAndStatus(Long communityId, MarketDonation.SharingMode sharingMode, MarketDonation.DonationStatus status, Pageable pageable);
    List<MarketDonation> findByDonorIdOrderByCreatedAtDesc(Long donorId);
}
