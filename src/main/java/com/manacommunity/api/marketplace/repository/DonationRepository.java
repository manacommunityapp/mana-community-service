package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.Donation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    Page<Donation> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, Donation.DonationStatus status, Pageable pageable);

    List<Donation> findByDonorIdOrderByCreatedAtDesc(Long donorId);
}
