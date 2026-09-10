package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.marketplace.dto.MarketReportRequest;
import com.manacommunity.api.marketplace.dto.MarketReportResponse;
import com.manacommunity.api.marketplace.entity.MarketListing;
import com.manacommunity.api.marketplace.entity.MarketReport;
import com.manacommunity.api.marketplace.repository.MarketListingRepository;
import com.manacommunity.api.marketplace.repository.MarketReportRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketModerationService {

    private final MarketReportRepository reportRepository;
    private final MarketListingRepository listingRepository;

    public Page<MarketReportResponse> getPendingReports(Long communityId, Pageable pageable) {
        return reportRepository.findByCommunityIdAndStatus(communityId, MarketReport.ReportStatus.PENDING, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public MarketReportResponse reportListing(MarketReportRequest.Create req, AppUser reporter, Community community) {
        MarketListing listing = listingRepository.findById(req.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + req.getListingId()));

        MarketReport report = MarketReport.builder()
                .listing(listing)
                .reporter(reporter)
                .community(community != null ? community : listing.getCommunity())
                .reason(req.getReason())
                .details(req.getDetails())
                .status(MarketReport.ReportStatus.PENDING)
                .build();

        return toResponse(reportRepository.save(report));
    }

    @Transactional
    public MarketReportResponse moderateReport(Long reportId, MarketReportRequest.Moderate req, AppUser admin) {
        MarketReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));

        report.setStatus(req.getStatus());
        report.setActionTaken(req.getActionTaken());
        report.setModeratedBy(admin);

        if (req.getStatus() == MarketReport.ReportStatus.ACTION_TAKEN && report.getListing() != null) {
            report.getListing().setStatus(MarketListing.ListingStatus.DELETED);
            listingRepository.save(report.getListing());
        }

        return toResponse(reportRepository.save(report));
    }

    private MarketReportResponse toResponse(MarketReport r) {
        return MarketReportResponse.builder()
                .id(r.getId())
                .listingId(r.getListing().getId())
                .listingTitle(r.getListing().getTitle())
                .reporterId(r.getReporter().getId())
                .reporterName(r.getReporter().getFullName() != null ? r.getReporter().getFullName() : r.getReporter().getUsername())
                .communityId(r.getCommunity() != null ? r.getCommunity().getId() : null)
                .reason(r.getReason())
                .details(r.getDetails())
                .status(r.getStatus())
                .actionTaken(r.getActionTaken())
                .moderatedByName(r.getModeratedBy() != null ? r.getModeratedBy().getFullName() : null)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
