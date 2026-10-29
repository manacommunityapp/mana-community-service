package com.manacommunity.api.privacy;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import com.manacommunity.api.exception.ManaCommunityException;

import java.util.Optional;

/**
 * DPDP Act 2023 s.6 — reusable guard that checks whether the latest consent
 * record for a given user + purpose is granted before allowing a processing
 * operation to proceed.
 *
 * Usage example (biometric enrolment):
 *   consentGuard.requireConsent(userId, "BIOMETRIC",
 *       "Biometric enrolment requires explicit consent. Please grant it in Privacy Centre.");
 */
@Component
@RequiredArgsConstructor
public class ConsentGuard {

    private final ConsentRecordRepository consentRepo;

    /**
     * Throws {@link ManaCommunityException} (HTTP 403) if the user has not
     * granted explicit consent for the given purpose.
     *
     * Essential processing (purpose="ESSENTIAL") always passes — it cannot be revoked.
     */
    public void requireConsent(Long userId, String purpose, String userFacingMessage) {
        if ("ESSENTIAL".equalsIgnoreCase(purpose)) return;

        Optional<ConsentRecord> latest = consentRepo.findLatestByUserIdAndPurpose(userId, purpose.toUpperCase());
        boolean granted = latest.map(ConsentRecord::getGranted).orElse(false);

        if (!granted) {
            throw new ManaCommunityException(
                userFacingMessage != null ? userFacingMessage
                    : "Consent for " + purpose + " is required. Please update your preferences in Privacy Centre.",
                HttpStatus.FORBIDDEN,
                "CONSENT_REQUIRED"
            );
        }
    }

    /** Returns true if consent is granted; false otherwise (non-throwing variant). */
    public boolean hasConsent(Long userId, String purpose) {
        if ("ESSENTIAL".equalsIgnoreCase(purpose)) return true;
        return consentRepo.findLatestByUserIdAndPurpose(userId, purpose.toUpperCase())
                .map(ConsentRecord::getGranted)
                .orElse(false);
    }
}
