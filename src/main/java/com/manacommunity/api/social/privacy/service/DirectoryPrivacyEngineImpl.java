package com.manacommunity.api.social.privacy.service;

import com.manacommunity.api.social.privacy.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DirectoryPrivacyEngineImpl implements DirectoryPrivacyEngine {

    // Fast in-memory cache simulating L1 Caffeine / Redis cache for user privacy settings
    private final Map<Long, UserPrivacyMatrix> privacyCache = new ConcurrentHashMap<>();

    private static final UserPrivacyMatrix DEFAULT_MATRIX = UserPrivacyMatrix.builder()
            .phoneScope(VisibilityScope.VERIFIED_OWNERS_ONLY)
            .vehicleScope(VisibilityScope.EVERYONE_IN_SOCIETY)
            .flatScope(VisibilityScope.EVERYONE_IN_SOCIETY)
            .emailScope(VisibilityScope.HIDDEN_COMPLETELY)
            .build();

    @Override
    public void registerPrivacyMatrix(UserPrivacyMatrix matrix) {
        if (matrix != null && matrix.getUserId() != null) {
            privacyCache.put(matrix.getUserId(), matrix);
        }
    }

    @Override
    public SanitizedDirectoryEntry sanitizeRecord(ViewerContext viewer, ResidentDirectoryEntry target) {
        if (target == null) {
            return null;
        }

        // Cross-community access is completely blocked (all sensitive attributes nullified)
        if (viewer == null || !Objects.equals(viewer.getCommunityId(), target.getCommunityId())) {
            return SanitizedDirectoryEntry.builder()
                    .userId(target.getUserId())
                    .residentName(target.getResidentName())
                    .phoneNumber(null)
                    .vehicleNumber(null)
                    .flatNumber(null)
                    .email(null)
                    .phoneMasked(true)
                    .vehicleMasked(true)
                    .flatMasked(true)
                    .emailMasked(true)
                    .build();
        }

        UserPrivacyMatrix matrix = privacyCache.getOrDefault(target.getUserId(), DEFAULT_MATRIX);

        // 1. Phone Number Resolution
        boolean canViewPhone = canViewField(viewer, target, matrix.getPhoneScope());
        String phone;
        boolean phoneMasked = false;
        if (canViewPhone) {
            phone = target.getPhoneNumber();
        } else if (matrix.getPhoneScope() == VisibilityScope.HIDDEN_COMPLETELY) {
            phone = null;
            phoneMasked = true;
        } else {
            phone = maskPhoneNumber(target.getPhoneNumber());
            phoneMasked = true;
        }

        // 2. Vehicle Plate Resolution
        boolean canViewVehicle = canViewField(viewer, target, matrix.getVehicleScope());
        String vehicle;
        boolean vehicleMasked = false;
        if (canViewVehicle) {
            vehicle = target.getVehicleNumber();
        } else if (matrix.getVehicleScope() == VisibilityScope.HIDDEN_COMPLETELY) {
            vehicle = null;
            vehicleMasked = true;
        } else {
            vehicle = maskVehicleNumber(target.getVehicleNumber());
            vehicleMasked = true;
        }

        // 3. Flat Number Resolution
        boolean canViewFlat = canViewField(viewer, target, matrix.getFlatScope());
        String flat;
        boolean flatMasked = false;
        if (canViewFlat) {
            flat = target.getFlatNumber();
        } else {
            flat = null; // Flat details completely hidden if unauthorized
            flatMasked = true;
        }

        // 4. Email Resolution
        boolean canViewEmail = canViewField(viewer, target, matrix.getEmailScope());
        String email = canViewEmail ? target.getEmail() : null;
        boolean emailMasked = !canViewEmail;

        return SanitizedDirectoryEntry.builder()
                .userId(target.getUserId())
                .residentName(target.getResidentName())
                .phoneNumber(phone)
                .vehicleNumber(vehicle)
                .flatNumber(flat)
                .email(email)
                .phoneMasked(phoneMasked)
                .vehicleMasked(vehicleMasked)
                .flatMasked(flatMasked)
                .emailMasked(emailMasked)
                .build();
    }

    @Override
    public List<SanitizedDirectoryEntry> sanitizeBatch(ViewerContext viewer, List<ResidentDirectoryEntry> targets) {
        if (targets == null || targets.isEmpty()) {
            return Collections.emptyList();
        }
        List<SanitizedDirectoryEntry> results = new ArrayList<>(targets.size());
        for (ResidentDirectoryEntry target : targets) {
            results.add(sanitizeRecord(viewer, target));
        }
        return results;
    }

    @Override
    public boolean canViewField(ViewerContext viewer, ResidentDirectoryEntry target, VisibilityScope scope) {
        // Self-access or Society Admin can view all fields
        if (viewer != null && viewer.getViewerUserId() != null &&
                (viewer.getViewerUserId().equals(target.getUserId()) || viewer.isSocietyAdmin())) {
            return true;
        }

        if (scope == null || scope == VisibilityScope.HIDDEN_COMPLETELY) {
            return false;
        }

        // Cross-community access is always rejected
        if (viewer == null || !Objects.equals(viewer.getCommunityId(), target.getCommunityId())) {
            return false;
        }

        return switch (scope) {
            case EVERYONE_IN_SOCIETY -> true;
            case VERIFIED_OWNERS_ONLY -> viewer.isVerifiedOwner();
            case MANAGEMENT_COMMITTEE_ONLY -> viewer.isManagementCommittee() || viewer.isSocietyAdmin();
            case CLUB_MEMBERS_ONLY -> hasSharedClubMembership(viewer.getJoinedClubIds(), target.getJoinedClubIds());
            case HIDDEN_COMPLETELY -> false;
        };
    }

    private boolean hasSharedClubMembership(Set<Long> viewerClubs, Set<Long> targetClubs) {
        if (viewerClubs == null || viewerClubs.isEmpty() || targetClubs == null || targetClubs.isEmpty()) {
            return false;
        }
        // O(min(|A|, |B|)) set disjoint check
        return !Collections.disjoint(viewerClubs, targetClubs);
    }

    @Override
    public String maskPhoneNumber(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }
        String clean = rawPhone.replaceAll("[^0-9]", "");
        if (clean.length() < 7) {
            return "***";
        }
        // e.g. +91 98*** **210
        String prefix = clean.substring(0, Math.min(4, clean.length() - 3));
        String suffix = clean.substring(clean.length() - 3);
        return "+91 " + prefix + "*** ***" + suffix;
    }

    @Override
    public String maskVehicleNumber(String rawPlate) {
        if (rawPlate == null || rawPlate.isBlank()) {
            return null;
        }
        String clean = rawPlate.replaceAll("\\s+", "").toUpperCase();
        if (clean.length() < 6) {
            return "**-****";
        }
        // e.g. KA01AB1234 -> KA 01 ** 1234
        String state = clean.substring(0, 2);
        String rto = clean.substring(2, Math.min(4, clean.length()));
        String number = clean.substring(Math.max(4, clean.length() - 4));
        return state + " " + rto + " ** " + number;
    }
}
