package com.manacommunity.api.mw.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.mw.dto.*;
import com.manacommunity.api.mw.entity.*;
import com.manacommunity.api.mw.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MwWorkforcePoolService {

    private final MwPoolListingRepository listingRepo;
    private final MwPoolApplicationRepository applicationRepo;
    private final MwEngagementContractRepository contractRepo;

    // ── Employer: manage own listings ────────────────────────────────────────

    public MwPoolListing createListing(Long orgId, Long createdByUserId, CreatePoolListingRequest req) {
        MwPoolListing listing = MwPoolListing.builder()
                .organizationId(orgId)
                .createdBy(createdByUserId)
                .title(req.getTitle())
                .description(req.getDescription())
                .requiredSkills(req.getRequiredSkills())
                .shiftDate(req.getShiftDate())
                .shiftStart(req.getShiftStart())
                .shiftEnd(req.getShiftEnd())
                .location(req.getLocation())
                .payRate(req.getPayRate())
                .payType(req.getPayType())
                .maxWorkers(req.getMaxWorkers())
                .build();
        return listingRepo.save(listing);
    }

    public List<MwPoolListing> getOrgListings(Long orgId) {
        return listingRepo.findByOrganizationIdOrderByShiftDateAsc(orgId);
    }

    public MwPoolListing getListing(Long listingId) {
        return listingRepo.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + listingId));
    }

    @Transactional
    public MwPoolListing closeListing(Long orgId, Long listingId) {
        MwPoolListing listing = getListing(listingId);
        if (!listing.getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("Not your listing");
        }
        listing.setStatus("CLOSED");
        return listingRepo.save(listing);
    }

    // ── Worker: browse & apply ────────────────────────────────────────────────

    public List<MwPoolListing> browseOpenListings(Long workerOrgId) {
        // Show open listings from other orgs (workers apply to external shifts)
        if (workerOrgId != null) {
            return listingRepo.findOpenListingsForBrowse(LocalDate.now(), workerOrgId);
        }
        return listingRepo.findAllOpenListings(LocalDate.now());
    }

    @Transactional
    public MwPoolApplication applyToListing(Long listingId, Long manaUserId, ApplyToListingRequest req) {
        if (applicationRepo.existsByListingIdAndManaUserId(listingId, manaUserId)) {
            throw new IllegalStateException("Already applied to this listing");
        }
        MwPoolListing listing = getListing(listingId);
        if (!"OPEN".equals(listing.getStatus())) {
            throw new IllegalStateException("Listing is not open for applications");
        }
        MwPoolApplication app = MwPoolApplication.builder()
                .listing(listing)
                .manaUserId(manaUserId)
                .passportId(req.getPassportId())
                .workerNote(req.getWorkerNote())
                .build();
        return applicationRepo.save(app);
    }

    public List<MwPoolApplication> getMyApplications(Long manaUserId) {
        return applicationRepo.findByManaUserId(manaUserId);
    }

    // ── Employer: respond to applications ────────────────────────────────────

    public List<MwPoolApplication> getListingApplications(Long orgId, Long listingId) {
        MwPoolListing listing = getListing(listingId);
        if (!listing.getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("Not your listing");
        }
        return applicationRepo.findByListingId(listingId);
    }

    @Transactional
    public MwPoolApplication respondToApplication(Long orgId, Long applicationId, RespondToApplicationRequest req) {
        MwPoolApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        if (!app.getListing().getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("Not your listing");
        }

        String decision = req.getDecision().toUpperCase();
        if (!"ACCEPTED".equals(decision) && !"REJECTED".equals(decision)) {
            throw new IllegalArgumentException("Decision must be ACCEPTED or REJECTED");
        }

        app.setStatus(decision);
        app.setEmployerNote(req.getEmployerNote());
        app.setRespondedAt(LocalDateTime.now());
        applicationRepo.save(app);

        if ("ACCEPTED".equals(decision)) {
            // Auto-create engagement contract
            MwEngagementContract contract = MwEngagementContract.builder()
                    .application(app)
                    .termsSummary(buildTermsSummary(app))
                    .build();
            contractRepo.save(contract);

            // Auto-fill listing if max workers reached
            MwPoolListing listing = app.getListing();
            int acceptedCount = applicationRepo.countByListingIdAndStatus(listing.getId(), "ACCEPTED");
            listing.setAcceptedCount(acceptedCount);
            if (acceptedCount >= listing.getMaxWorkers()) {
                listing.setStatus("FILLED");
            }
            listingRepo.save(listing);
        }

        return app;
    }

    // ── Worker: consent & contract ────────────────────────────────────────────

    @Transactional
    public MwEngagementContract consentToContract(Long manaUserId, Long contractId) {
        MwEngagementContract contract = contractRepo.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        if (!contract.getApplication().getManaUserId().equals(manaUserId)) {
            throw new AccessDeniedException("Not your contract");
        }

        contract.setWorkerConsented(true);
        contract.setConsentedAt(LocalDateTime.now());
        return contractRepo.save(contract);
    }

    // ── Employer: complete engagement ─────────────────────────────────────────

    @Transactional
    public MwEngagementContract completeEngagement(Long orgId, Long contractId, CompleteEngagementRequest req) {
        MwEngagementContract contract = contractRepo.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        if (!contract.getApplication().getListing().getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("Not your contract");
        }

        contract.setHoursWorked(req.getHoursWorked());
        contract.setAmountDue(req.getAmountDue());
        if (req.getTermsSummary() != null) {
            contract.setTermsSummary(req.getTermsSummary());
        }
        contract.setStatus("COMPLETED");
        contract.setCompletedAt(LocalDateTime.now());
        return contractRepo.save(contract);
    }

    public MwEngagementContract getContractByApplication(Long applicationId) {
        return contractRepo.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found for application: " + applicationId));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildTermsSummary(MwPoolApplication app) {
        MwPoolListing l = app.getListing();
        return String.format("Shift: %s %s-%s | Pay: %s %s | Location: %s",
                l.getShiftDate(), l.getShiftStart(), l.getShiftEnd(),
                l.getPayRate(), l.getPayType(), l.getLocation());
    }
}
