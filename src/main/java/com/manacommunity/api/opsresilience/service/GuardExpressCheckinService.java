package com.manacommunity.api.opsresilience.service;

import com.manacommunity.api.opsresilience.entity.GuardExpressCheckin;
import com.manacommunity.api.opsresilience.repository.GuardExpressCheckinRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Low-Digital-Literacy / Rainy-Gate Express Check-In Service:
 * - Zero-typing modes: COURIER_ICON, OTP_6_DIGIT, QR_PASS, GUARD_OVERRIDE_PHOTO.
 * - Idempotent on (gateId, clientSeqId) so offline optimistic FIFO queues never duplicate entries.
 * - Enforces mandatory photo capture and Management Committee audit flagging on manual overrides.
 */
@Service
@RequiredArgsConstructor
public class GuardExpressCheckinService {

    private static final Set<String> ALLOWED_MODES = Set.of(
            "COURIER_ICON", "OTP_6_DIGIT", "QR_PASS", "GUARD_OVERRIDE_PHOTO"
    );

    private final GuardExpressCheckinRepository checkinRepo;

    @Builder
    public record ExpressCheckinRequest(
            Long communityId,
            String gateId,
            Long guardId,
            Long clientSeqId,
            String entryMode,
            String brandCode,
            String flatNo,
            String photoUrl
    ) {}

    @Builder
    public record ExpressCheckinResponse(
            GuardExpressCheckin checkin,
            boolean idempotentReplay,
            String audioCue
    ) {}

    @Transactional
    public ExpressCheckinResponse recordExpressCheckin(ExpressCheckinRequest req) {
        if (req.communityId() == null || req.gateId() == null || req.gateId().isBlank()
                || req.guardId() == null || req.clientSeqId() == null || req.flatNo() == null || req.flatNo().isBlank()) {
            throw new IllegalArgumentException("communityId, gateId, guardId, clientSeqId, and flatNo are required");
        }

        String gate = req.gateId().trim().toUpperCase(Locale.ROOT);
        String mode = req.entryMode() != null ? req.entryMode().trim().toUpperCase(Locale.ROOT) : "";
        if (!ALLOWED_MODES.contains(mode)) {
            throw new IllegalArgumentException("Unsupported express entry mode: " + req.entryMode());
        }

        var existing = checkinRepo.findByGateIdAndClientSeqId(gate, req.clientSeqId());
        if (existing.isPresent()) {
            GuardExpressCheckin found = existing.get();
            return ExpressCheckinResponse.builder()
                    .checkin(found)
                    .idempotentReplay(true)
                    .audioCue(found.getAudioCue())
                    .build();
        }

        boolean isOverride = "GUARD_OVERRIDE_PHOTO".equals(mode);
        if (isOverride && (req.photoUrl() == null || req.photoUrl().isBlank())) {
            throw new IllegalArgumentException("GUARD_OVERRIDE_PHOTO requires mandatory photo evidence URL");
        }

        String audioCue = isOverride ? "OVERRIDE_REVIEW_BEEP" : "ALLOW_CHIME";
        GuardExpressCheckin saved = checkinRepo.save(GuardExpressCheckin.builder()
                .communityId(req.communityId())
                .gateId(gate)
                .guardId(req.guardId())
                .clientSeqId(req.clientSeqId())
                .entryMode(mode)
                .brandCode(req.brandCode() != null ? req.brandCode().trim().toUpperCase(Locale.ROOT) : null)
                .flatNo(req.flatNo().trim().toUpperCase(Locale.ROOT))
                .photoUrl(req.photoUrl())
                .audioCue(audioCue)
                .requiresMcReview(isOverride)
                .build());

        return ExpressCheckinResponse.builder()
                .checkin(saved)
                .idempotentReplay(false)
                .audioCue(audioCue)
                .build();
    }

    @Transactional(readOnly = true)
    public List<GuardExpressCheckin> listPendingMcOverrides(Long communityId) {
        return checkinRepo.findByCommunityIdAndRequiresMcReviewTrueOrderByCreatedAtDesc(communityId);
    }
}
