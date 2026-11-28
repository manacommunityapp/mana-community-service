package com.manacommunity.api.social.privacy;

import com.manacommunity.api.social.privacy.model.*;
import com.manacommunity.api.social.privacy.service.DirectoryPrivacyEngine;
import com.manacommunity.api.social.privacy.service.DirectoryPrivacyEngineImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DirectoryPrivacyEngineTest {

    private DirectoryPrivacyEngine privacyEngine;
    private static final Long COMMUNITY_ID = 101L;

    @BeforeEach
    void setUp() {
        privacyEngine = new DirectoryPrivacyEngineImpl();

        // Target User 1: Phone restricted to VERIFIED_OWNERS_ONLY, Flat to EVERYONE, Email HIDDEN
        privacyEngine.registerPrivacyMatrix(UserPrivacyMatrix.builder()
                .userId(201L)
                .communityId(COMMUNITY_ID)
                .phoneScope(VisibilityScope.VERIFIED_OWNERS_ONLY)
                .vehicleScope(VisibilityScope.EVERYONE_IN_SOCIETY)
                .flatScope(VisibilityScope.EVERYONE_IN_SOCIETY)
                .emailScope(VisibilityScope.HIDDEN_COMPLETELY)
                .build());

        // Target User 2: Flat restricted to CLUB_MEMBERS_ONLY, Phone HIDDEN_COMPLETELY
        privacyEngine.registerPrivacyMatrix(UserPrivacyMatrix.builder()
                .userId(202L)
                .communityId(COMMUNITY_ID)
                .phoneScope(VisibilityScope.HIDDEN_COMPLETELY)
                .vehicleScope(VisibilityScope.EVERYONE_IN_SOCIETY)
                .flatScope(VisibilityScope.CLUB_MEMBERS_ONLY)
                .emailScope(VisibilityScope.EVERYONE_IN_SOCIETY)
                .build());
    }

    @Test
    @DisplayName("Verified owner sees unmasked phone, non-owner sees masked phone")
    void testVerifiedOwnerPhoneVisibility() {
        ResidentDirectoryEntry target = ResidentDirectoryEntry.builder()
                .userId(201L)
                .communityId(COMMUNITY_ID)
                .residentName("Dr. Suresh Sharma")
                .phoneNumber("+919876543210")
                .vehicleNumber("KA01AB1234")
                .flatNumber("Tower A - 402")
                .email("suresh@example.com")
                .build();

        // 1. Viewer is a Verified Owner
        ViewerContext ownerViewer = ViewerContext.builder()
                .viewerUserId(501L)
                .communityId(COMMUNITY_ID)
                .isVerifiedOwner(true)
                .build();

        SanitizedDirectoryEntry ownerResult = privacyEngine.sanitizeRecord(ownerViewer, target);
        assertNotNull(ownerResult);
        assertEquals("+919876543210", ownerResult.getPhoneNumber(), "Verified owner must see full phone number");
        assertFalse(ownerResult.isPhoneMasked());
        assertNull(ownerResult.getEmail(), "Hidden email must remain null");

        // 2. Viewer is an Unverified Resident / Tenant
        ViewerContext tenantViewer = ViewerContext.builder()
                .viewerUserId(502L)
                .communityId(COMMUNITY_ID)
                .isVerifiedOwner(false)
                .build();

        SanitizedDirectoryEntry tenantResult = privacyEngine.sanitizeRecord(tenantViewer, target);
        assertNotNull(tenantResult);
        assertTrue(tenantResult.isPhoneMasked(), "Non-owner must receive masked phone");
        assertTrue(tenantResult.getPhoneNumber().contains("***"), "Phone should be masked with asterisks");
        assertEquals("Tower A - 402", tenantResult.getFlatNumber(), "Flat is EVERYONE_IN_SOCIETY so must be visible");
    }

    @Test
    @DisplayName("Club members see restricted fields, non-members do not")
    void testClubMembershipScopedVisibility() {
        ResidentDirectoryEntry target = ResidentDirectoryEntry.builder()
                .userId(202L)
                .communityId(COMMUNITY_ID)
                .residentName("Pooja Nair")
                .phoneNumber("+919111222333")
                .flatNumber("Tower B - 1204")
                .vehicleNumber("KA05CD9988")
                .email("pooja@example.com")
                .joinedClubIds(Set.of(10L, 25L)) // Badminton Club (10) and Running Club (25)
                .build();

        // 1. Viewer shares Badminton Club (10)
        ViewerContext clubMateViewer = ViewerContext.builder()
                .viewerUserId(503L)
                .communityId(COMMUNITY_ID)
                .isVerifiedOwner(false)
                .joinedClubIds(Set.of(10L, 99L))
                .build();

        SanitizedDirectoryEntry clubResult = privacyEngine.sanitizeRecord(clubMateViewer, target);
        assertEquals("Tower B - 1204", clubResult.getFlatNumber(), "Shared club member must see flat number");
        assertNull(clubResult.getPhoneNumber(), "HIDDEN_COMPLETELY phone must remain null even to club members");

        // 2. Viewer has no shared clubs
        ViewerContext strangerViewer = ViewerContext.builder()
                .viewerUserId(504L)
                .communityId(COMMUNITY_ID)
                .isVerifiedOwner(false)
                .joinedClubIds(Set.of(77L))
                .build();

        SanitizedDirectoryEntry strangerResult = privacyEngine.sanitizeRecord(strangerViewer, target);
        assertNull(strangerResult.getFlatNumber(), "Non-club member must not see flat number");
        assertTrue(strangerResult.isFlatMasked());
    }

    @Test
    @DisplayName("Cross-community access is completely blocked")
    void testCrossCommunityIsolation() {
        ResidentDirectoryEntry target = ResidentDirectoryEntry.builder()
                .userId(201L)
                .communityId(COMMUNITY_ID)
                .residentName("Dr. Suresh Sharma")
                .phoneNumber("+919876543210")
                .flatNumber("Tower A - 402")
                .build();

        // Viewer from different community (999L)
        ViewerContext externalViewer = ViewerContext.builder()
                .viewerUserId(600L)
                .communityId(999L)
                .isVerifiedOwner(true)
                .build();

        SanitizedDirectoryEntry result = privacyEngine.sanitizeRecord(externalViewer, target);
        assertNull(result.getFlatNumber());
        assertNull(result.getPhoneNumber());
        assertTrue(result.isPhoneMasked());
        assertTrue(result.isFlatMasked());
    }

    @Test
    @DisplayName("Management Committee scope hides restricted fields from neighbors and exposes them to MC members")
    void testManagementCommitteeOnlyVisibility() {
        privacyEngine.registerPrivacyMatrix(UserPrivacyMatrix.builder()
                .userId(203L)
                .communityId(COMMUNITY_ID)
                .phoneScope(VisibilityScope.MANAGEMENT_COMMITTEE_ONLY)
                .vehicleScope(VisibilityScope.MANAGEMENT_COMMITTEE_ONLY)
                .flatScope(VisibilityScope.EVERYONE_IN_SOCIETY)
                .emailScope(VisibilityScope.HIDDEN_COMPLETELY)
                .build());

        ResidentDirectoryEntry target = ResidentDirectoryEntry.builder()
                .userId(203L)
                .communityId(COMMUNITY_ID)
                .residentName("Vikram Rao")
                .phoneNumber("+919800112233")
                .vehicleNumber("TS09EF4321")
                .flatNumber("Tower C - 901")
                .email("vikram@example.com")
                .build();

        ViewerContext neighborOwner = ViewerContext.builder()
                .viewerUserId(510L).communityId(COMMUNITY_ID)
                .isVerifiedOwner(true).isManagementCommittee(false).build();
        SanitizedDirectoryEntry neighborView = privacyEngine.sanitizeRecord(neighborOwner, target);
        assertTrue(neighborView.isPhoneMasked());
        assertTrue(neighborView.isVehicleMasked());
        assertNotEquals("+919800112233", neighborView.getPhoneNumber());
        assertNotEquals("TS09EF4321", neighborView.getVehicleNumber());

        ViewerContext mcMember = ViewerContext.builder()
                .viewerUserId(511L).communityId(COMMUNITY_ID)
                .isVerifiedOwner(true).isManagementCommittee(true).build();
        SanitizedDirectoryEntry mcView = privacyEngine.sanitizeRecord(mcMember, target);
        assertFalse(mcView.isPhoneMasked());
        assertFalse(mcView.isVehicleMasked());
        assertEquals("+919800112233", mcView.getPhoneNumber());
        assertEquals("TS09EF4321", mcView.getVehicleNumber());
        assertNull(mcView.getEmail());
    }
}
