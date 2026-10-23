package com.manacommunity.api.resident.service.impl;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.resident.dto.*;
import com.manacommunity.api.resident.model.CommunityFlat;
import com.manacommunity.api.resident.model.FlatDependentMember;
import com.manacommunity.api.resident.model.FlatMembership;
import com.manacommunity.api.resident.repository.CommunityFlatRepository;
import com.manacommunity.api.resident.repository.FlatDependentMemberRepository;
import com.manacommunity.api.resident.repository.FlatMembershipRepository;
import com.manacommunity.api.resident.service.ResidentOnboardingService;
import com.manacommunity.api.service.FieldEncryptionService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.model.FamilyMember;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.repository.FamilyMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResidentOnboardingServiceImpl implements ResidentOnboardingService {

    private final CommunityFlatRepository communityFlatRepository;
    private final FlatMembershipRepository flatMembershipRepository;
    private final FlatDependentMemberRepository flatDependentMemberRepository;
    private final AppUserRepository appUserRepository;
    private final CommunityRepository communityRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final FieldEncryptionService fieldEncryptionService;

    @Override
    @Transactional
    public ResidentOnboardingResponse onboardResident(ResidentOnboardingRequest request, AppUser currentUser) {
        Community community = communityRepository.findById(request.getCommunityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Community not found with ID: " + request.getCommunityId()));

        String tower = request.getTowerBlock().trim();
        String flatNo = request.getFlatNumber().trim();

        // 1. Find or create flat container
        CommunityFlat flat = communityFlatRepository
                .findByCommunityIdAndTowerBlockIgnoreCaseAndFlatNumberIgnoreCase(community.getId(), tower, flatNo)
                .orElseGet(() -> {
                    CommunityFlat newFlat = CommunityFlat.builder()
                            .community(community)
                            .towerBlock(tower)
                            .flatNumber(flatNo)
                            .floorNumber(request.getFloorNumber())
                            .verificationStatus("PENDING")
                            .build();
                    return communityFlatRepository.save(newFlat);
                });

        // 2. Update user profile information
        currentUser.setFullName(request.getFullName().trim());
        currentUser.setCommunity(community);
        currentUser.setFlatNo(flatNo);
        currentUser.setBlock(tower);
        currentUser.setTower(tower);
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            currentUser.setEmail(request.getEmail().trim());
        }
        if (request.getDateOfBirth() != null) {
            currentUser.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            currentUser.setGender(request.getGender().toUpperCase());
        }
        if (request.getProfilePicUrl() != null && !request.getProfilePicUrl().isBlank()) {
            currentUser.setProfilePicUrl(request.getProfilePicUrl());
        }
        if (request.getGovtIdType() != null && !request.getGovtIdType().isBlank()) {
            currentUser.setGovtIdType(request.getGovtIdType());
        }
        if (request.getGovtIdNumber() != null && !request.getGovtIdNumber().isBlank()) {
            String encId = fieldEncryptionService.isEnabled()
                    ? fieldEncryptionService.encrypt(request.getGovtIdNumber().trim())
                    : request.getGovtIdNumber().trim();
            currentUser.setGovtIdNumber(encId);
        }
        currentUser.setKycStatus("PENDING");
        currentUser.setOccupancyStatus(request.getResidentType() != null ? request.getResidentType().toUpperCase() : "OWNER");
        currentUser.setResidentType("Resident");

        appUserRepository.save(currentUser);

        // 3. Create or update flat membership
        Optional<FlatMembership> existingMembership = flatMembershipRepository.findByUserIdAndFlatId(currentUser.getId(), flat.getId());
        FlatMembership membership;
        if (existingMembership.isPresent()) {
            membership = existingMembership.get();
            membership.setResidentType(request.getResidentType() != null ? request.getResidentType().toUpperCase() : "OWNER");
            membership.setRelationshipToFlat(request.getRelationshipToFlat() != null ? request.getRelationshipToFlat() : "SELF");
            if (request.getIsPrimaryResident() != null) {
                membership.setIsPrimaryResident(request.getIsPrimaryResident());
            }
        } else {
            // Check if this is the first member of the flat
            boolean isFirstMember = flatMembershipRepository.findByFlatId(flat.getId()).isEmpty();
            membership = FlatMembership.builder()
                    .user(currentUser)
                    .flat(flat)
                    .residentType(request.getResidentType() != null ? request.getResidentType().toUpperCase() : "OWNER")
                    .relationshipToFlat(request.getRelationshipToFlat() != null ? request.getRelationshipToFlat() : "SELF")
                    .isPrimaryResident(Boolean.TRUE.equals(request.getIsPrimaryResident()) || isFirstMember)
                    .appAccessStatus("FULL_ACCESS")
                    .build();
        }
        flatMembershipRepository.save(membership);

        return ResidentOnboardingResponse.builder()
                .userId(currentUser.getId())
                .fullName(currentUser.getFullName())
                .phone(currentUser.getPhone())
                .email(currentUser.getEmail())
                .communityId(community.getId())
                .communityName(community.getName())
                .flatId(flat.getId())
                .towerBlock(flat.getTowerBlock())
                .flatNumber(flat.getFlatNumber())
                .residentType(membership.getResidentType())
                .relationshipToFlat(membership.getRelationshipToFlat())
                .isPrimaryResident(membership.getIsPrimaryResident())
                .verificationStatus(flat.getVerificationStatus())
                .appAccessStatus(membership.getAppAccessStatus())
                .message("Resident onboarding successful. Flat verification is pending society admin review.")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FamilyRosterResponse getFamilyRoster(Long flatId, AppUser currentUser) {
        CommunityFlat flat = communityFlatRepository.findById(flatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Flat not found with ID: " + flatId));

        // Security check: currentUser must belong to this flat or be admin
        boolean isMember = flatMembershipRepository.findByUserIdAndFlatId(currentUser.getId(), flatId).isPresent();
        if (!isMember && !currentUser.hasRole("ADMIN") && !currentUser.hasRole("SUPER_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this household roster.");
        }

        List<FlatMembership> memberships = flatMembershipRepository.findByFlatIdWithUser(flatId);
        List<FamilyRosterResponse.AdultMemberDto> adults = memberships.stream()
                .map(m -> FamilyRosterResponse.AdultMemberDto.builder()
                        .membershipId(m.getId())
                        .userId(m.getUser().getId())
                        .fullName(m.getUser().getFullName())
                        .phone(m.getUser().getPhone())
                        .email(m.getUser().getEmail())
                        .residentType(m.getResidentType())
                        .relationshipToFlat(m.getRelationshipToFlat())
                        .isPrimaryResident(m.getIsPrimaryResident())
                        .appAccessStatus(m.getAppAccessStatus())
                        .profilePicUrl(m.getUser().getProfilePicUrl())
                        .build())
                .collect(Collectors.toList());

        List<FlatDependentMember> dependents = flatDependentMemberRepository.findActiveByFlatId(flatId);
        LocalDate today = LocalDate.now();
        List<FamilyRosterResponse.ChildMemberDto> children = dependents.stream()
                .map(d -> {
                    int age = d.getDateOfBirth() != null ? Period.between(d.getDateOfBirth(), today).getYears() : 0;
                    return FamilyRosterResponse.ChildMemberDto.builder()
                            .dependentId(d.getId())
                            .guardianUserId(d.getGuardianUser().getId())
                            .guardianName(d.getGuardianUser().getFullName())
                            .fullName(d.getFullName())
                            .dateOfBirth(d.getDateOfBirth())
                            .age(age)
                            .gender(d.getGender())
                            .relationship(d.getRelationship())
                            .isParentManaged(d.getIsParentManaged())
                            .sportsEligible(d.getSportsEligible())
                            .status(d.getStatus())
                            .profilePicUrl(d.getProfilePicUrl())
                            .build();
                })
                .collect(Collectors.toList());

        return FamilyRosterResponse.builder()
                .flatId(flat.getId())
                .towerBlock(flat.getTowerBlock())
                .flatNumber(flat.getFlatNumber())
                .communityId(flat.getCommunity().getId())
                .communityName(flat.getCommunity().getName())
                .verificationStatus(flat.getVerificationStatus())
                .adults(adults)
                .children(children)
                .build();
    }

    @Override
    @Transactional
    public FamilyRosterResponse addAdultMember(AddAdultMemberRequest request, AppUser currentUser) {
        CommunityFlat flat = communityFlatRepository.findById(request.getFlatId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Flat not found with ID: " + request.getFlatId()));

        String normalizedPhone = request.getPhone().trim();

        // 1. Find or create shadow account for adult user
        AppUser targetUser = appUserRepository.findByPhone(normalizedPhone).orElseGet(() -> {
            String tempEmail = "invited_" + UUID.randomUUID().toString().substring(0, 8) + "@manacommunity.app";
            AppUser shadow = AppUser.builder()
                    .fullName(request.getFullName().trim())
                    .phone(normalizedPhone)
                    .email(request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail().trim() : tempEmail)
                    .passwordHash("INVITED_PENDING_OTP")
                    .dateOfBirth(LocalDate.of(2000, 1, 1))
                    .gender("OTHER")
                    .community(flat.getCommunity())
                    .tower(flat.getTowerBlock())
                    .block(flat.getTowerBlock())
                    .flatNo(flat.getFlatNumber())
                    .occupancyStatus("FAMILY_MEMBER")
                    .residentType("Resident")
                    .kycStatus("PENDING")
                    .isActive(true)
                    .build();
            return appUserRepository.save(shadow);
        });

        // 2. Link targetUser to this flat
        Optional<FlatMembership> existingOpt = flatMembershipRepository.findByUserIdAndFlatId(targetUser.getId(), flat.getId());
        if (existingOpt.isPresent()) {
            FlatMembership existing = existingOpt.get();
            existing.setRelationshipToFlat(request.getRelationship().toUpperCase());
            existing.setAppAccessStatus(Boolean.TRUE.equals(request.getAllowAppAccess()) ? "FULL_ACCESS" : "READ_ONLY");
            flatMembershipRepository.save(existing);
        } else {
            FlatMembership newMembership = FlatMembership.builder()
                    .user(targetUser)
                    .flat(flat)
                    .residentType("FAMILY_MEMBER")
                    .relationshipToFlat(request.getRelationship().toUpperCase())
                    .isPrimaryResident(false)
                    .appAccessStatus(Boolean.TRUE.equals(request.getAllowAppAccess()) ? "FULL_ACCESS" : "READ_ONLY")
                    .invitePhone(normalizedPhone)
                    .invitedBy(currentUser.getId())
                    .build();
            flatMembershipRepository.save(newMembership);
        }

        log.info("[Household] Added adult member phone={} to flatId={} by userId={}", normalizedPhone, flat.getId(), currentUser.getId());
        return getFamilyRoster(flat.getId(), currentUser);
    }

    @Override
    @Transactional
    public FamilyRosterResponse addChildDependent(AddChildMemberRequest request, AppUser currentUser) {
        CommunityFlat flat = communityFlatRepository.findById(request.getFlatId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Flat not found with ID: " + request.getFlatId()));

        FlatDependentMember dependent = FlatDependentMember.builder()
                .flat(flat)
                .guardianUser(currentUser)
                .fullName(request.getFullName().trim())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender().toUpperCase())
                .relationship(request.getRelationship().toUpperCase())
                .isParentManaged(true)
                .emergencyContact(request.getEmergencyContact() != null ? request.getEmergencyContact() : currentUser.getPhone())
                .bloodGroup(request.getBloodGroup())
                .profilePicUrl(request.getProfilePicUrl())
                .sportsEligible(request.getSportsEligible() != null ? request.getSportsEligible() : true)
                .status("ACTIVE")
                .build();

        flatDependentMemberRepository.save(dependent);

        // Backward compatibility: also sync to legacy FamilyMember table for older modules
        try {
            int age = Period.between(request.getDateOfBirth(), LocalDate.now()).getYears();
            FamilyMember legacyMember = FamilyMember.builder()
                    .user(currentUser)
                    .community(flat.getCommunity())
                    .name(request.getFullName().trim())
                    .relation(request.getRelationship())
                    .dob(request.getDateOfBirth().toString())
                    .age(age)
                    .gender(request.getGender())
                    .bloodGroup(request.getBloodGroup())
                    .status("ACTIVE")
                    .avatar(request.getProfilePicUrl())
                    .build();
            familyMemberRepository.save(legacyMember);
        } catch (Exception e) {
            log.warn("[Household] Legacy FamilyMember sync error: {}", e.getMessage());
        }

        log.info("[Household] Added child dependent name={} to flatId={} by guardianId={}", request.getFullName(), flat.getId(), currentUser.getId());
        return getFamilyRoster(flat.getId(), currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPropertySummaryDto> getUserProperties(AppUser currentUser) {
        List<FlatMembership> memberships = flatMembershipRepository.findByUserIdWithFlatAndCommunity(currentUser.getId());
        List<UserPropertySummaryDto> result = new ArrayList<>();

        for (FlatMembership m : memberships) {
            CommunityFlat flat = m.getFlat();
            int adultCount = flatMembershipRepository.findByFlatId(flat.getId()).size();
            int childCount = flatDependentMemberRepository.findActiveByFlatId(flat.getId()).size();

            result.add(UserPropertySummaryDto.builder()
                    .membershipId(m.getId())
                    .flatId(flat.getId())
                    .towerBlock(flat.getTowerBlock())
                    .flatNumber(flat.getFlatNumber())
                    .communityId(flat.getCommunity().getId())
                    .communityName(flat.getCommunity().getName())
                    .communityCity(flat.getCommunity().getCity())
                    .residentType(m.getResidentType())
                    .relationshipToFlat(m.getRelationshipToFlat())
                    .isPrimaryResident(m.getIsPrimaryResident())
                    .verificationStatus(flat.getVerificationStatus())
                    .appAccessStatus(m.getAppAccessStatus())
                    .totalFamilyMembers(adultCount + childCount)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional
    public void removeDependent(Long dependentId, AppUser currentUser) {
        FlatDependentMember member = flatDependentMemberRepository.findById(dependentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dependent not found with ID: " + dependentId));

        member.setStatus("INACTIVE");
        flatDependentMemberRepository.save(member);
        log.info("[Household] Deactivated dependentId={} by userId={}", dependentId, currentUser.getId());
    }

    @Override
    @Transactional
    public void removeAdultMember(Long membershipId, AppUser currentUser) {
        FlatMembership membership = flatMembershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found with ID: " + membershipId));

        if (Boolean.TRUE.equals(membership.getIsPrimaryResident()) && membership.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Primary resident cannot remove their own membership directly. Reassign primary status first.");
        }

        membership.setAppAccessStatus("REVOKED");
        flatMembershipRepository.save(membership);
        log.info("[Household] Revoked membershipId={} by userId={}", membershipId, currentUser.getId());
    }
}
