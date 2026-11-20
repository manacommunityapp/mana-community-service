package com.manacommunity.api.communityops.pass;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DigitalPassService {

    private final DigitalPassRepository repo;

    @Transactional
    public DigitalPass generate(Long communityId, Long createdBy, DigitalPassRequest req) {
        Instant now = Instant.now();
        Instant validUntil = switch (req.passType()) {
            case DELIVERY -> now.plus(Duration.ofHours(4));
            case GUEST -> now.plus(Duration.ofHours(24));
            case DOMESTIC_WORKER -> now.plus(Duration.ofDays(30));
            case CONTRACTOR -> now.plus(Duration.ofDays(7));
            case VEHICLE -> now.plus(Duration.ofHours(12));
        };
        return repo.save(DigitalPass.builder()
            .communityId(communityId)
            .createdBy(createdBy)
            .flatNumber(req.flatNumber())
            .passType(req.passType())
            .visitorName(req.visitorName())
            .vehicleNumber(req.vehicleNumber())
            .purpose(req.purpose())
            .token(UUID.randomUUID().toString().replace("-", ""))
            .status(PassStatus.ACTIVE)
            .validFrom(now)
            .validUntil(validUntil)
            .build());
    }

    @Transactional
    public DigitalPass scan(String token, Long guardUserId) {
        DigitalPass pass = repo.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Pass not found"));
        if (pass.getStatus() == PassStatus.REVOKED) throw new IllegalStateException("Pass has been revoked");
        if (pass.getStatus() == PassStatus.EXPIRED || Instant.now().isAfter(pass.getValidUntil()))
            throw new IllegalStateException("Pass has expired");
        // One-time passes: DELIVERY, GUEST
        if (pass.getPassType() == PassType.DELIVERY || pass.getPassType() == PassType.GUEST) {
            pass.setStatus(PassStatus.USED);
            pass.setUsedAt(Instant.now());
            pass.setUsedByGuard(guardUserId);
        }
        return repo.save(pass);
    }

    @Transactional
    public void revoke(Long passId, Long revokedBy) {
        DigitalPass pass = repo.findById(passId)
            .orElseThrow(() -> new IllegalArgumentException("Pass not found"));
        pass.setStatus(PassStatus.REVOKED);
        repo.save(pass);
    }

    public List<DigitalPass> getMyActivePasses(Long userId) {
        return repo.findByCreatedByAndStatusAndValidUntilAfter(userId, PassStatus.ACTIVE, Instant.now());
    }

    public List<DigitalPass> getActivePassesForFlat(Long communityId, String flatNumber) {
        return repo.findByCommunityIdAndFlatNumberAndStatusAndValidUntilAfter(communityId, flatNumber, PassStatus.ACTIVE, Instant.now());
    }
}
