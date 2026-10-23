package com.manacommunity.api.intelligence.service;

import com.manacommunity.api.intelligence.model.GraphProfile;
import com.manacommunity.api.intelligence.model.ProfileVisibility;
import com.manacommunity.api.privacy.UserPrivacySettings;
import com.manacommunity.api.privacy.UserPrivacySettingsRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PrivacyEnforcementFilter {

    private final UserPrivacySettingsRepository privacySettingsRepo;

    public enum AccessLevel {
        FULL,           // Self or authorized admin
        DISCOVERABLE,   // Visible with privacy rules applied
        RESTRICTED,     // Masked contact & restricted info
        DENIED          // Private / Hidden
    }

    /**
     * Determines whether the target profile is visible to the requester.
     */
    public AccessLevel evaluateAccess(AppUser requester, GraphProfile target) {
        if (target == null || !Boolean.TRUE.equals(target.getIsActive())) {
            return AccessLevel.DENIED;
        }

        // Self view
        if (target.getUser() != null && target.getUser().getId().equals(requester.getId())) {
            return AccessLevel.FULL;
        }

        // Target has set visibility to PRIVATE -> Never discoverable to other residents
        if (target.getVisibility() == ProfileVisibility.PRIVATE) {
            return AccessLevel.DENIED;
        }

        boolean isSameTower = isSameTower(requester, target);

        // If target visibility is NEIGHBORS only -> Requester must be in the same tower
        if (target.getVisibility() == ProfileVisibility.NEIGHBORS) {
            if (isSameTower) {
                return AccessLevel.DISCOVERABLE;
            } else {
                return AccessLevel.DENIED; // Not in same tower, completely filtered out
            }
        }

        // PUBLIC visibility -> Discoverable across entire community
        return AccessLevel.DISCOVERABLE;
    }

    public boolean isPhoneVisible(AppUser requester, GraphProfile target) {
        if (target.getUser() == null) return false;
        if (target.getUser().getId().equals(requester.getId())) return true;

        Optional<UserPrivacySettings> settings = privacySettingsRepo.findByUserId(target.getUser().getId());
        if (settings.isPresent()) {
            UserPrivacySettings s = settings.get();
            if (Boolean.TRUE.equals(s.getShowPhoneToNeighbours())) {
                return isSameTower(requester, target);
            }
            return false;
        }
        return false;
    }

    public boolean isEmailVisible(AppUser requester, GraphProfile target) {
        if (target.getUser() == null) return false;
        if (target.getUser().getId().equals(requester.getId())) return true;

        Optional<UserPrivacySettings> settings = privacySettingsRepo.findByUserId(target.getUser().getId());
        if (settings.isPresent()) {
            UserPrivacySettings s = settings.get();
            if (Boolean.TRUE.equals(s.getShowEmailToNeighbours())) {
                return isSameTower(requester, target);
            }
            return false;
        }
        return false;
    }

    public boolean isFlatVisible(AppUser requester, GraphProfile target) {
        if (target.getUser() == null) return true;
        if (target.getUser().getId().equals(requester.getId())) return true;

        Optional<UserPrivacySettings> settings = privacySettingsRepo.findByUserId(target.getUser().getId());
        return settings.map(s -> Boolean.TRUE.equals(s.getShowFlatInDirectory())).orElse(true);
    }

    private boolean isSameTower(AppUser requester, GraphProfile target) {
        String reqTower = requester.getTower() != null ? requester.getTower() : requester.getBlock();
        String targetTower = target.getTower() != null ? target.getTower() : (target.getUser() != null ? target.getUser().getTower() : null);

        if (reqTower == null || targetTower == null) return false;
        return reqTower.trim().equalsIgnoreCase(targetTower.trim());
    }
}