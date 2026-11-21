package com.manacommunity.api.homeservices.service;

import com.manacommunity.api.homeservices.entity.*;
import com.manacommunity.api.homeservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HsQualityService {

    private final HsQualityCheckRepository qcRepo;
    private final HsBookingPhotoRepository photoRepo;
    private final HsWarrantyRepository warrantyRepo;
    private final HsReworkRequestRepository reworkRepo;

    @Transactional
    public HsQualityCheck recordCheck(HsQualityCheck check) {
        if (check.getOverallScore() < 1 || check.getOverallScore() > 5)
            throw new IllegalArgumentException("Score must be 1-5");
        return qcRepo.save(check);
    }

    public HsQualityCheck getByBooking(Long bookingId) {
        return qcRepo.findByBookingId(bookingId).orElse(null);
    }

    @Transactional
    public HsBookingPhoto uploadPhoto(HsBookingPhoto photo) {
        return photoRepo.save(photo);
    }

    public List<HsBookingPhoto> photos(Long bookingId) {
        return photoRepo.findByBookingId(bookingId);
    }

    @Transactional
    public HsWarranty issueWarranty(Long bookingId, Long communityId, int warrantyDays) {
        HsWarranty w = HsWarranty.builder()
            .bookingId(bookingId).communityId(communityId)
            .warrantyDays(warrantyDays)
            .expiresAt(LocalDate.now().plusDays(warrantyDays))
            .status("ACTIVE").build();
        return warrantyRepo.save(w);
    }

    @Transactional
    public HsWarranty claimWarranty(Long bookingId, String reason) {
        HsWarranty w = warrantyRepo.findByBookingId(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("No warranty found"));
        if (!"ACTIVE".equals(w.getStatus())) throw new IllegalStateException("Warranty not active");
        if (w.getExpiresAt().isBefore(LocalDate.now())) {
            w.setStatus("EXPIRED"); warrantyRepo.save(w);
            throw new IllegalStateException("Warranty has expired");
        }
        w.setStatus("CLAIMED");
        w.setClaimReason(reason);
        w.setClaimedAt(Instant.now());
        return warrantyRepo.save(w);
    }

    @Transactional
    public HsReworkRequest requestRework(HsReworkRequest req) {
        return reworkRepo.save(req);
    }

    @Transactional
    public HsReworkRequest approveRework(Long reworkId) {
        HsReworkRequest r = reworkRepo.findById(reworkId)
            .orElseThrow(() -> new IllegalArgumentException("Rework request not found"));
        r.setStatus("APPROVED");
        return reworkRepo.save(r);
    }
}
