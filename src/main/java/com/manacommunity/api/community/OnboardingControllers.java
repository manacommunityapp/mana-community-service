package com.manacommunity.api.community;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Onboarding-related community and KYC endpoints.
 * Two controllers in one compilation unit for convenience.
 */

// ── Community Lookup ──────────────────────────────────────────
@Slf4j
@RestController
@RequestMapping("/api/communities")
@RequiredArgsConstructor
class CommunityLookupController {

    @PersistenceContext
    private final EntityManager em;

    /**
     * GET /api/communities/lookup?code=XXXX
     *
     * Used by the mobile onboarding Step 2 to show a preview of the
     * community before the user completes registration.
     *
     * Returns: { id, name, city, state, area, memberCount, inviteCode }
     * 404 if no community with this invite code exists.
     */
    @GetMapping("/lookup")
    public ResponseEntity<Map<String, Object>> lookupByCode(
            @RequestParam("code") String code) {

        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Object community;
        try {
            community = em.createQuery(
                "SELECT c FROM Community c WHERE UPPER(c.inviteCode) = UPPER(:code)")
                .setParameter("code", code.trim())
                .getSingleResult();
        } catch (NoResultException e) {
            return ResponseEntity.notFound().build();
        }

        var cls  = community.getClass();
        Long id  = (Long) getField(community, cls, "id");

        // Count active members
        Long memberCount = em.createQuery(
            "SELECT COUNT(u) FROM AppUser u WHERE u.community.id=:cid AND u.isActive=true",
            Long.class)
            .setParameter("cid", id)
            .getSingleResult();

        return ResponseEntity.ok(Map.of(
            "id",          id,
            "name",        orEmpty(getField(community, cls, "name")),
            "city",        orEmpty(getField(community, cls, "city")),
            "state",       orEmpty(getField(community, cls, "state")),
            "area",        orEmpty(getField(community, cls, "area")),
            "inviteCode",  orEmpty(getField(community, cls, "inviteCode")),
            "memberCount", memberCount != null ? memberCount : 0L
        ));
    }

    private Object getField(Object obj, Class<?> cls, String field) {
        try {
            var f = cls.getDeclaredField(field);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception e) {
            try {
                String getter = "get" + field.substring(0, 1).toUpperCase() + field.substring(1);
                return cls.getDeclaredMethod(getter).invoke(obj);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private String orEmpty(Object val) {
        return val != null ? val.toString() : "";
    }
}

// ── KYC Controller ────────────────────────────────────────────
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
class KycController {

    @PersistenceContext
    private final EntityManager em;

    private final LoggedInUserService loggedInUserService;
    private final com.manacommunity.api.service.FieldEncryptionService fieldEncryptionService;
    private final com.manacommunity.api.kyc.service.KycIdNumberProtector kycIdNumberProtector;

    /**
     * PUT /api/auth/kyc
     *
     * Called by the mobile onboarding Step 4 (verify.tsx) after the user
     * has uploaded their government ID images.
     *
     * Body: {
     *   govtIdType:        "AADHAAR" | "PAN" | "PASSPORT" | "VOTER_ID" | "DRIVING_LICENSE"
     *   govtIdNumber:      "123456789012"
     *   documentFrontUrl:  "https://..."
     *   documentBackUrl?:  "https://..."    (optional — not required for PAN/Passport)
     * }
     *
     * Sets kycStatus = "PENDING" (admin must review and approve via the admin panel).
     * Returns the updated UserProfileResponse.
     */
    @PutMapping("/kyc")
    @Transactional
    public ResponseEntity<Map<String, Object>> submitKyc(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {

        // This method sits under the permitAll /api/auth/** matcher. It was relying on
        // loggedInUserService.resolve() dereferencing a null principal to keep anonymous
        // callers out, which is an accident rather than a control. Reject explicitly.
        if (principal == null || principal.getId() == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required"));
        }

        AppUser user = loggedInUserService.resolve(principal);

        String idType    = body.getOrDefault("govtIdType",       "").toString().trim();
        String frontUrl  = body.getOrDefault("documentFrontUrl", "").toString().trim();
        String backUrl   = body.getOrDefault("documentBackUrl",  "").toString().trim();

        // The client hashes the government ID before it leaves the device, so the raw
        // number must never arrive here. Refuse it rather than hashing it server-side,
        // which would quietly restore plaintext transmission.
        Object rawSubmitted = body.get("govtIdNumber");
        kycIdNumberProtector.rejectRawIdNumber(
                rawSubmitted == null ? null : rawSubmitted.toString(),
                "govtIdNumber", "govtIdNumberHash' and 'govtIdLast4");

        Object hashRaw  = body.get("govtIdNumberHash");
        Object last4Raw = body.get("govtIdLast4");
        String clientDigest = kycIdNumberProtector.requireClientDigest(
                hashRaw == null ? null : hashRaw.toString().trim(), "govtIdNumberHash");
        String idLast4 = kycIdNumberProtector.requireLast4(
                last4Raw == null ? null : last4Raw.toString().trim(), "govtIdLast4");

        if (idType.isBlank() || frontUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "govtIdType and documentFrontUrl are required"
            ));
        }

        // The client's bare SHA-256 of a 12-digit number is brute-forceable offline;
        // re-protect it under the server-held pepper before it is stored.
        String storedHash = kycIdNumberProtector.protectForStorage(clientDigest, "govtIdNumberHash");

        // Persist KYC document info using a separate KYC table if it exists,
        // or store directly on AppUser if the fields are there.
        // Using native SQL for maximum compatibility with the existing schema.
        // The failure here used to be swallowed with a warning, after which the user was
        // still marked PENDING — so a resident saw "submitted" with no document row behind
        // it. If the document cannot be written, the submission has not happened.
        try {
            em.createNativeQuery(
                "INSERT INTO kyc_document (user_id, id_type, document_type, id_number_hash, id_number_last4," +
                "                          front_url, front_image_url, back_url, back_image_url, status, submitted_at)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', NOW())" +
                " ON CONFLICT (user_id) DO UPDATE SET" +
                "   id_type=EXCLUDED.id_type, document_type=EXCLUDED.document_type," +
                "   id_number_hash=EXCLUDED.id_number_hash, id_number_last4=EXCLUDED.id_number_last4," +
                "   front_url=EXCLUDED.front_url, front_image_url=EXCLUDED.front_image_url," +
                "   back_url=EXCLUDED.back_url, back_image_url=EXCLUDED.back_image_url," +
                "   status='PENDING', submitted_at=NOW()")
                .setParameter(1, user.getId())
                .setParameter(2, idType)
                .setParameter(3, idType)
                .setParameter(4, storedHash)
                .setParameter(5, idLast4)
                .setParameter(6, frontUrl)
                .setParameter(7, frontUrl)
                .setParameter(8, backUrl.isBlank() ? null : backUrl)
                .setParameter(9, backUrl.isBlank() ? null : backUrl)
                .executeUpdate();
        } catch (Exception e) {
            log.error("[KYC] Failed to persist kyc_document for userId={}: {}", user.getId(), e.toString());
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "KYC submission failed. Nothing was recorded — please try again."
            ));
        }

        // Only now is the submission real.
        try {
            user.setKycStatus("PENDING");
            // The raw number no longer reaches this server, so there is nothing to mirror
            // onto AppUser beyond the type; the hash and last 4 live on kyc_document.
            try { user.getClass().getDeclaredMethod("setGovtIdType", String.class).invoke(user, idType); } catch (Exception ignored) {}
            em.merge(user);
        } catch (Exception e) {
            log.error("[KYC] Failed to update AppUser kycStatus: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "KYC submission failed"));
        }

        log.info("[KYC] Submitted for userId={} idType={}", user.getId(), idType);

        // Return updated user profile
        return ResponseEntity.ok(Map.of(
            "id",         user.getId(),
            "name",       user.getFullName(),
            "email",      user.getEmail(),
            "kycStatus",  "PENDING",
            "message",    "KYC submitted successfully. Your admin will review and approve your account."
        ));
    }
}
