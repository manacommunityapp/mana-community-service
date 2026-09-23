package com.manacommunity.api.homeservice.service;

import com.manacommunity.api.homeservice.dto.AdminAnalyticsDto;
import com.manacommunity.api.homeservice.model.enums.HomeServiceReportStatus;
import com.manacommunity.api.homeservice.model.enums.HomeServiceRequirementStatus;
import com.manacommunity.api.homeservice.model.enums.HomeServiceVerificationStatus;
import com.manacommunity.api.homeservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.HashMap;

@Service("homeServiceAdminService")
@RequiredArgsConstructor
public class HomeServiceAdminService {
    private final HomeServiceWorkerRepository workerRepository;
    private final HomeServiceBookingRepository bookingRepository;
    private final HomeServiceRequirementRepository requirementRepository;
    private final HomeServiceReportRepository reportRepository;

    public AdminAnalyticsDto getAnalytics(String communityId) {
        var workers = workerRepository.findByCommunityIdAndActiveTrue(communityId);
        long verified = workers.stream().filter(w -> w.getVerificationStatus() == HomeServiceVerificationStatus.VERIFIED).count();
        long pending = workers.stream().filter(w -> w.getVerificationStatus() == HomeServiceVerificationStatus.PENDING).count();

        long openReqs = requirementRepository.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, HomeServiceRequirementStatus.OPEN).size();
        long activeIncidents = reportRepository.findByCommunityIdOrderByCreatedAtDesc(communityId).stream()
                .filter(r -> r.getStatus() == HomeServiceReportStatus.OPEN || r.getStatus() == HomeServiceReportStatus.INVESTIGATING)
                .count();

        return AdminAnalyticsDto.builder()
                .totalWorkers(workers.size())
                .verifiedWorkers(verified)
                .pendingVerifications(pending)
                .activeBookings(bookingRepository.count())
                .openRequirements(openReqs)
                .activeIncidents(activeIncidents)
                .workersByCategory(new HashMap<>())
                .build();
    }
}
