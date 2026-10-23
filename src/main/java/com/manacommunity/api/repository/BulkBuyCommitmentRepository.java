package com.manacommunity.api.repository;

import com.manacommunity.api.model.BulkBuyCommitment;
import com.manacommunity.api.model.CommitmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BulkBuyCommitmentRepository extends JpaRepository<BulkBuyCommitment, Long> {

    List<BulkBuyCommitment> findByCampaignIdOrderByCreatedAtDesc(Long campaignId);

    Optional<BulkBuyCommitment> findByCampaignIdAndUserIdAndStatusNot(
            Long campaignId, Long userId, CommitmentStatus status);
}
