package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.marketplace.entity.MarketDataDeletionRequest;
import com.manacommunity.api.marketplace.repository.MarketDataDeletionRequestRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketPrivacyService {

    private final MarketDataDeletionRequestRepository deletionRequestRepository;

    public List<MarketDataDeletionRequest> getUserDeletionRequests(Long userId) {
        return deletionRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public MarketDataDeletionRequest requestDataDeletion(AppUser user, String reason) {
        MarketDataDeletionRequest req = MarketDataDeletionRequest.builder()
                .user(user)
                .reason(reason != null ? reason : "User requested data deletion under DPDP Act")
                .status(MarketDataDeletionRequest.DeletionStatus.PENDING)
                .build();
        return deletionRequestRepository.save(req);
    }

    @Transactional
    public MarketDataDeletionRequest processDeletionRequest(Long requestId, MarketDataDeletionRequest.DeletionStatus newStatus, AppUser admin) {
        MarketDataDeletionRequest req = deletionRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Deletion request not found"));

        req.setStatus(newStatus);
        req.setProcessedBy(admin);
        req.setProcessedAt(LocalDateTime.now());

        return deletionRequestRepository.save(req);
    }

    /**
     * Helper to mask resident tower/apartment details for non-transacting community members
     */
    public String maskTowerInfo(String tower, String flatNumber) {
        if (tower == null && flatNumber == null) return "Resident";
        String towerPart = (tower != null && !tower.isBlank()) ? "Tower " + tower.charAt(0) + "**" : "Tower *";
        return towerPart + " (Verified Resident)";
    }
}
