package com.manacommunity.api.user.service.impl;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.dto.FamilyMemberRequest;
import com.manacommunity.api.user.dto.FamilyMemberResponse;
import com.manacommunity.api.user.dto.FamilyMemberSlimResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.model.FamilyMember;
import com.manacommunity.api.user.repository.FamilyMemberRepository;
import com.manacommunity.api.user.service.FamilyMemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.manacommunity.api.sports.model.SportsEventRegistration;
import com.manacommunity.api.sports.repository.SportsEventRegistrationRepository;

import static com.manacommunity.api.constants.PermissionConstants.ROLE_SUPER_ADMIN;

@Service
public class FamilyMemberServiceImpl implements FamilyMemberService {

    private final FamilyMemberRepository repository;
    private final CommunityRepository communityRepository;
    private final SportsEventRegistrationRepository sportsEventRegistrationRepository;

    public FamilyMemberServiceImpl(FamilyMemberRepository repository,
                                   CommunityRepository communityRepository,
                                   SportsEventRegistrationRepository sportsEventRegistrationRepository) {
        this.repository = repository;
        this.communityRepository = communityRepository;
        this.sportsEventRegistrationRepository = sportsEventRegistrationRepository;
    }

    private boolean isUserAuthorizedToManage(FamilyMember existing, AppUser currentUser) {
        if (existing == null || currentUser == null) return false;
        if (currentUser.hasRole(ROLE_SUPER_ADMIN)) return true;
        if (existing.getUser() != null && existing.getUser().getId() != null
                && existing.getUser().getId().equals(currentUser.getId())) {
            return true;
        }
        // Community Admin scoping
        if (currentUser.hasRole("ADMIN") || currentUser.hasRole("COMMUNITY_ADMIN")) {
            Long userCommId = currentUser.getCommunity() != null ? currentUser.getCommunity().getId() : null;
            Long targetCommId = existing.getCommunity() != null ? existing.getCommunity().getId() : null;
            return userCommId != null && userCommId.equals(targetCommId);
        }
        return false;
    }

    private FamilyMemberResponse toResponse(FamilyMember m) {
        if (m == null) return null;
        return FamilyMemberResponse.builder()
                .id(m.getId())
                .userId(m.getUser() != null ? m.getUser().getId() : null)
                .communityId(m.getCommunity() != null ? m.getCommunity().getId() : null)
                .name(m.getName())
                .relation(m.getRelation())
                .age(m.getAge())
                .gender(m.getGender())
                .dob(m.getDob())
                .phone(m.getPhone())
                .email(m.getEmail())
                .bloodGroup(m.getBloodGroup())
                .gothram(m.getGothram())
                .gotram(m.getGothram())
                .emergencyContact(m.getEmergencyContact())
                .isDevotee(m.getIsDevotee())
                .avatar(m.getAvatar())
                .notes(m.getNotes())
                .status(m.getStatus())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> getFamilyMembersResponse(AppUser user, Long communityId) {
        List<FamilyMember> list = getFamilyMembers(user, communityId);
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> getFamilyMembersByUserId(Long targetUserId) {
        if (targetUserId == null) return Collections.emptyList();
        return repository.findByUserIdOrderByCreatedAtAsc(targetUserId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyMember> getFamilyMembers(AppUser user, Long communityId) {
        if (user == null || user.getId() == null) {
            if (communityId != null) {
                return repository.findByCommunityIdOrderByCreatedAtAsc(communityId);
            }
            return Collections.emptyList();
        }
        List<FamilyMember> list = repository.findByUserIdOrderByCreatedAtAsc(user.getId());
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            boolean hasSelf = list.stream().anyMatch(m ->
                    (m.getRelation() != null && (m.getRelation().equalsIgnoreCase("Self")
                            || m.getRelation().equalsIgnoreCase("Self (Head)")
                            || m.getRelation().equalsIgnoreCase("Head")))
                    || (m.getName() != null && m.getName().trim().equalsIgnoreCase(user.getFullName().trim()))
            );
            if (!hasSelf) {
                int age = 30;
                if (user.getDateOfBirth() != null) {
                    age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
                }
                boolean isFemale = "FEMALE".equalsIgnoreCase(user.getGender()) || "F".equalsIgnoreCase(user.getGender());
                String avatar = (user.getProfilePicUrl() != null && !user.getProfilePicUrl().isBlank())
                        ? user.getProfilePicUrl()
                        : (isFemale ? "👩" : "👤");

                String familyGothram = list.stream()
                        .map(FamilyMember::getGothram)
                        .filter(g -> g != null && !g.isBlank())
                        .findFirst()
                        .orElse(null);

                Community comm = (user.getCommunity() != null)
                        ? user.getCommunity()
                        : (communityId != null ? communityRepository.findById(communityId).orElse(null) : null);

                FamilyMember selfMember = FamilyMember.builder()
                        .id(null)
                        .user(user)
                        .community(comm)
                        .name(user.getFullName().trim())
                        .relation("Self (Head)")
                        .age(age)
                        .dob(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null)
                        .gender(user.getGender() != null ? user.getGender() : (isFemale ? "FEMALE" : "MALE"))
                        .avatar(avatar)
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .gothram(familyGothram)
                        .emergencyContact(false)
                        .isDevotee(true)
                        .status("ACTIVE")
                        .build();

                List<FamilyMember> updatedList = new java.util.ArrayList<>();
                updatedList.add(selfMember);
                updatedList.addAll(list);
                list = updatedList;
            }
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyMemberSlimResponse> getSlimFamilyMembers(Long targetUserId, AppUser currentUser) {
        Long resolvedId = targetUserId;
        if (resolvedId == null) {
            resolvedId = (currentUser != null) ? currentUser.getId() : null;
        }
        if (resolvedId == null) return Collections.emptyList();

        // Enforce access control for non-admin users
        if (currentUser != null && !currentUser.hasRole(ROLE_SUPER_ADMIN)
                && !currentUser.hasRole("ADMIN") && !currentUser.hasRole("COMMUNITY_ADMIN")) {
            resolvedId = currentUser.getId();
        }

        return repository.findByUserIdOrderByCreatedAtAsc(resolvedId).stream()
                .filter(m -> !isSelf(m))
                .map(m -> FamilyMemberSlimResponse.builder()
                        .id(m.getId())
                        .name(m.getName())
                        .gothram(m.getGothram())
                        .relation(m.getRelation())
                        .phone(m.getPhone())
                        .gender(m.getGender())
                        .age(m.getAge())
                        .dob(m.getDob())
                        .email(m.getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyMemberSlimResponse> getSlimFamilyMembers(Long userId) {
        if (userId == null) return Collections.emptyList();
        return repository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .filter(m -> !isSelf(m))
                .map(m -> FamilyMemberSlimResponse.builder()
                        .id(m.getId())
                        .name(m.getName())
                        .gothram(m.getGothram())
                        .relation(m.getRelation())
                        .phone(m.getPhone())
                        .gender(m.getGender())
                        .age(m.getAge())
                        .dob(m.getDob())
                        .email(m.getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    private boolean isSelf(FamilyMember m) {
        if (m == null) return false;
        String rel = m.getRelation();
        if (rel != null && (rel.equalsIgnoreCase("SELF") || rel.equalsIgnoreCase("HEAD") || rel.toUpperCase().contains("SELF"))) {
            return true;
        }
        return false;
    }



    private Integer calculateAgeFromDob(String dobStr) {
        if (dobStr == null || dobStr.isBlank()) return null;
        try {
            String clean = dobStr.trim();
            if (clean.length() >= 10) {
                clean = clean.substring(0, 10);
            }
            LocalDate birthDate = LocalDate.parse(clean);
            return Period.between(birthDate, LocalDate.now()).getYears();
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    @Transactional
    public FamilyMemberResponse addFamilyMember(FamilyMemberRequest request, AppUser user, Long communityId) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new InvalidInputException("Family member name is required");
        }

        Community comm = (user != null && user.getCommunity() != null)
                ? user.getCommunity()
                : (communityId != null ? communityRepository.findById(communityId).orElse(null) : null);

        String gender = (request.getGender() != null && !request.getGender().isBlank()) ? request.getGender().trim() : "Male";
        boolean isFemale = "Female".equalsIgnoreCase(gender) || "F".equalsIgnoreCase(gender);

        String effectiveDob = request.getEffectiveDob();
        Integer computedAge = request.getAge();
        if ((computedAge == null || computedAge <= 0) && effectiveDob != null) {
            computedAge = calculateAgeFromDob(effectiveDob);
        }
        if (computedAge == null || computedAge <= 0) {
            computedAge = 30;
        }

        String avatar = request.getAvatar();
        if (avatar == null || avatar.isBlank()) {
            avatar = isFemale ? (computedAge < 18 ? "👧" : "👩") : (computedAge < 18 ? "👦" : "👨");
        }

        FamilyMember member = FamilyMember.builder()
                .user(user)
                .community(comm)
                .name(request.getName().trim())
                .relation(request.getRelation() != null ? request.getRelation().trim() : "Family")
                .age(computedAge)
                .dob(effectiveDob)
                .gender(gender)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .bloodGroup(request.getBloodGroup() != null ? request.getBloodGroup().trim() : null)
                .gothram(request.getEffectiveGothram())
                .emergencyContact(Boolean.TRUE.equals(request.getEmergencyContact()))
                .isDevotee(request.getIsDevotee() == null || Boolean.TRUE.equals(request.getIsDevotee()))
                .avatar(avatar)
                .notes(request.getNotes())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();

        FamilyMember saved = repository.save(member);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public FamilyMember addFamilyMember(FamilyMember member, AppUser user, Long communityId) {
        FamilyMemberRequest req = FamilyMemberRequest.builder()
                .name(member.getName())
                .relation(member.getRelation())
                .age(member.getAge())
                .dob(member.getDob())
                .gender(member.getGender())
                .phone(member.getPhone())
                .email(member.getEmail())
                .bloodGroup(member.getBloodGroup())
                .gothram(member.getGothram())
                .emergencyContact(member.getEmergencyContact())
                .isDevotee(member.getIsDevotee())
                .avatar(member.getAvatar())
                .notes(member.getNotes())
                .status(member.getStatus())
                .build();
        FamilyMemberResponse resp = addFamilyMember(req, user, communityId);
        return repository.findById(resp.getId()).orElse(member);
    }

    @Override
    @Transactional
    public FamilyMemberResponse updateFamilyMember(Long id, FamilyMemberRequest request, AppUser user, Long communityId) {
        FamilyMember existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyMember", id));

        if (!isUserAuthorizedToManage(existing, user)) {
            throw new UnauthorizedActionException("You are not authorized to update this family member.");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            existing.setName(request.getName().trim());
        }
        if (request.getRelation() != null) existing.setRelation(request.getRelation().trim());
        if (request.getGender() != null) existing.setGender(request.getGender().trim());

        if (request.getDob() != null) {
            String trimmedDob = request.getDob().trim();
            if (!trimmedDob.isEmpty() && existing.getDob() != null && !trimmedDob.equals(existing.getDob().trim())) {
                boolean isAdmin = user != null && (user.hasRole(ROLE_SUPER_ADMIN)
                        || user.hasRole("ROLE_COMMUNITY_ADMIN")
                        || user.hasRole("ROLE_ADMIN")
                        || user.hasRole("COMMUNITY_ADMIN")
                        || user.hasRole("ADMIN"));
                if (!isAdmin) {
                    List<SportsEventRegistration.RegistrationStatus> activeStatuses = List.of(
                            SportsEventRegistration.RegistrationStatus.PENDING,
                            SportsEventRegistration.RegistrationStatus.REGISTERED,
                            SportsEventRegistration.RegistrationStatus.CONFIRMED
                    );
                    if (sportsEventRegistrationRepository.existsActiveRegistrationForFamilyMember(id, activeStatuses)) {
                        throw new InvalidInputException(
                                "Date of birth cannot be modified while this family member has active registrations in ongoing/upcoming tournaments.");
                    }
                }
            }
            existing.setDob(trimmedDob.isEmpty() ? null : trimmedDob);
            if (!trimmedDob.isEmpty() && (request.getAge() == null || request.getAge() <= 0)) {
                Integer calculatedAge = calculateAgeFromDob(trimmedDob);
                if (calculatedAge != null && calculatedAge > 0) {
                    existing.setAge(calculatedAge);
                }
            }
        }
        if (request.getAge() != null && request.getAge() > 0 && !request.getAge().equals(existing.getAge())) {
            if (existing.getDob() == null && existing.getAge() != null) {
                boolean isAdmin = user != null && (user.hasRole(ROLE_SUPER_ADMIN)
                        || user.hasRole("ROLE_COMMUNITY_ADMIN")
                        || user.hasRole("ROLE_ADMIN")
                        || user.hasRole("COMMUNITY_ADMIN")
                        || user.hasRole("ADMIN"));
                if (!isAdmin) {
                    List<SportsEventRegistration.RegistrationStatus> activeStatuses = List.of(
                            SportsEventRegistration.RegistrationStatus.PENDING,
                            SportsEventRegistration.RegistrationStatus.REGISTERED,
                            SportsEventRegistration.RegistrationStatus.CONFIRMED
                    );
                    if (sportsEventRegistrationRepository.existsActiveRegistrationForFamilyMember(id, activeStatuses)) {
                        throw new InvalidInputException(
                                "Age cannot be modified while this family member has active registrations in ongoing/upcoming tournaments.");
                    }
                }
            }
            existing.setAge(request.getAge());
        }

        if (request.getPhone() != null) existing.setPhone(request.getPhone().trim());
        if (request.getEmail() != null) existing.setEmail(request.getEmail().trim());
        if (request.getBloodGroup() != null) existing.setBloodGroup(request.getBloodGroup().trim());
        if (request.getEffectiveGothram() != null) existing.setGothram(request.getEffectiveGothram());
        if (request.getEmergencyContact() != null) existing.setEmergencyContact(request.getEmergencyContact());
        if (request.getIsDevotee() != null) existing.setIsDevotee(request.getIsDevotee());
        if (request.getAvatar() != null) existing.setAvatar(request.getAvatar());
        if (request.getNotes() != null) existing.setNotes(request.getNotes());
        if (request.getStatus() != null) existing.setStatus(request.getStatus());

        existing.setUpdatedAt(LocalDateTime.now());
        FamilyMember saved = repository.save(existing);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public FamilyMember updateFamilyMember(Long id, FamilyMember member, AppUser user, Long communityId) {
        FamilyMemberRequest req = FamilyMemberRequest.builder()
                .name(member.getName())
                .relation(member.getRelation())
                .age(member.getAge())
                .dob(member.getDob())
                .gender(member.getGender())
                .phone(member.getPhone())
                .email(member.getEmail())
                .bloodGroup(member.getBloodGroup())
                .gothram(member.getGothram())
                .emergencyContact(member.getEmergencyContact())
                .isDevotee(member.getIsDevotee())
                .avatar(member.getAvatar())
                .notes(member.getNotes())
                .status(member.getStatus())
                .build();
        FamilyMemberResponse resp = updateFamilyMember(id, req, user, communityId);
        return repository.findById(resp.getId()).orElse(member);
    }

    @Override
    @Transactional
    public void deleteFamilyMember(Long id, AppUser user, Long communityId) {
        FamilyMember existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyMember", id));

        if (!isUserAuthorizedToManage(existing, user)) {
            throw new UnauthorizedActionException("You are not authorized to delete this family member.");
        }

        List<SportsEventRegistration.RegistrationStatus> activeStatuses = List.of(
                SportsEventRegistration.RegistrationStatus.PENDING,
                SportsEventRegistration.RegistrationStatus.REGISTERED,
                SportsEventRegistration.RegistrationStatus.CONFIRMED
        );
        if (sportsEventRegistrationRepository.existsActiveRegistrationForFamilyMember(id, activeStatuses)) {
            throw new InvalidInputException(
                    "Cannot delete family member while they have active registrations in ongoing/upcoming tournaments. Please withdraw their registrations first.");
        }

        repository.delete(existing);
    }
}
