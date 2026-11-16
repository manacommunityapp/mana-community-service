package com.manacommunity.api.socialcircle.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.socialcircle.dto.KittyPartyCircleDtos.*;
import com.manacommunity.api.socialcircle.entity.KittyPartyCircle;
import com.manacommunity.api.socialcircle.entity.KittyPartyInvitation;
import com.manacommunity.api.socialcircle.entity.KittyPartyJoinRequest;
import com.manacommunity.api.socialcircle.entity.KittyPartyMember;
import com.manacommunity.api.socialcircle.repository.KittyPartyCircleRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyInvitationRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyJoinRequestRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyMemberRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.manacommunity.api.resident.model.CommunityFlat;
import com.manacommunity.api.resident.model.FlatMembership;
import com.manacommunity.api.resident.repository.FlatMembershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KittyPartyCircleService {

    private final KittyPartyCircleRepository circleRepository;
    private final KittyPartyMemberRepository memberRepository;
    private final KittyPartyInvitationRepository invitationRepository;
    private final KittyPartyJoinRequestRepository joinRequestRepository;
    private final AppUserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final KittyPartyAuthorizationService authorizationService;

    @Autowired(required = false)
    private FlatMembershipRepository flatMembershipRepository;

    @Transactional
    public CircleResponse createCircle(AppUser creator, CreateCircleRequest request) {
        Community community = creator.getCommunity();
        if (community == null) {
            throw new IllegalStateException("User does not belong to an active community");
        }

        KittyPartyCircle circle = KittyPartyCircle.builder()
                .community(community)
                .circleName(request.getCircleName().trim())
                .description(request.getDescription())
                .categoryCode(request.getCategoryCode() != null ? request.getCategoryCode().toUpperCase() : "KITTY_PARTY")
                .privacyType(request.getPrivacyType() != null ? request.getPrivacyType().toUpperCase() : "PRIVATE")
                .approvalRequired(request.getApprovalRequired() == null || request.getApprovalRequired())
                .maxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : 20)
                .allowExternalMembers(request.getAllowExternalMembers() == null || request.getAllowExternalMembers())
                .maxExternalMembers(request.getMaxExternalMembers() != null ? request.getMaxExternalMembers() : 3)
                .status("ACTIVE")
                .createdBy(creator)
                .build();

        KittyPartyCircle saved = circleRepository.save(circle);

        // Automatically enroll creator as OWNER and ADMIN
        KittyPartyMember ownerMember = KittyPartyMember.builder()
                .circle(saved)
                .user(creator)
                .community(community)
                .roleCode("OWNER")
                .membershipStatus("ACTIVE")
                .approvedBy(creator)
                .approvedAt(LocalDateTime.now())
                .build();
        memberRepository.save(ownerMember);

        log.info("Created KittyPartyCircle id={} name='{}' by user={}", saved.getId(), saved.getCircleName(), creator.getId());
        return mapToResponse(saved, "OWNER", 1L);
    }

    @Transactional(readOnly = true)
    public Page<CircleResponse> getCirclesForUser(Long userId, Pageable pageable) {
        Page<KittyPartyCircle> circles = circleRepository.findCirclesByMemberUserId(userId, pageable);
        return circles.map(c -> {
            String role = memberRepository.findByCircleIdAndUserId(c.getId(), userId)
                    .map(KittyPartyMember::getRoleCode).orElse(null);
            long count = memberRepository.countByCircleIdAndMembershipStatus(c.getId(), "ACTIVE");
            return mapToResponse(c, role, count);
        });
    }

    @Transactional(readOnly = true)
    public Page<CircleResponse> discoverCircles(Long communityId, String categoryCode, Pageable pageable) {
        Page<KittyPartyCircle> circles = circleRepository.findByCommunity(communityId, categoryCode, "ACTIVE", pageable);
        return circles.map(c -> {
            long count = memberRepository.countByCircleIdAndMembershipStatus(c.getId(), "ACTIVE");
            return mapToResponse(c, null, count);
        });
    }

    @Transactional(readOnly = true)
    public CircleResponse getCircleDetails(Long circleId, Long currentUserId) {
        KittyPartyCircle circle = circleRepository.findByIdAndDeletedAtIsNull(circleId)
                .orElseThrow(() -> new IllegalArgumentException("Circle not found with id: " + circleId));

        String role = memberRepository.findByCircleIdAndUserId(circleId, currentUserId)
                .map(KittyPartyMember::getRoleCode).orElse(null);

        long count = memberRepository.countByCircleIdAndMembershipStatus(circleId, "ACTIVE");
        return mapToResponse(circle, role, count);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getCircleMembers(Long circleId) {
        return memberRepository.findByCircleIdAndMembershipStatus(circleId, "ACTIVE").stream()
                .map(m -> {
                    String flatInfo = null;
                    if (flatMembershipRepository != null && m.getUser() != null) {
                        try {
                            List<FlatMembership> flats = flatMembershipRepository.findByUserIdWithFlatAndCommunity(m.getUser().getId());
                            if (!flats.isEmpty() && flats.get(0).getFlat() != null) {
                                CommunityFlat flat = flats.get(0).getFlat();
                                flatInfo = (flat.getTowerBlock() != null ? flat.getTowerBlock() + " - " : "") + flat.getFlatNumber();
                            }
                        } catch (Exception e) {
                            log.debug("Flat lookup skipped for user {}: {}", m.getUser().getId(), e.getMessage());
                        }
                    }
                    return MemberResponse.builder()
                            .memberId(m.getId())
                            .userId(m.getUser().getId())
                            .fullName(m.getUser().getFullName())
                            .phone(m.getUser().getPhone())
                            .roleCode(m.getRoleCode())
                            .membershipStatus(m.getMembershipStatus())
                            .joinedAt(m.getJoinedAt())
                            .flatUnit(flatInfo)
                            .isExternal(m.getIsExternal())
                            .sponsorUserId(m.getSponsorUser() != null ? m.getSponsorUser().getId() : null)
                            .sponsorName(m.getSponsorUser() != null ? m.getSponsorUser().getFullName() : null)
                            .externalSocietyName(m.getExternalSocietyName())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void requestToJoin(Long circleId, AppUser user, JoinRequestPayload payload) {
        KittyPartyCircle circle = circleRepository.findByIdAndDeletedAtIsNull(circleId)
                .orElseThrow(() -> new IllegalArgumentException("Circle not found"));

        if (memberRepository.existsByCircleIdAndUserIdAndMembershipStatus(circleId, user.getId(), "ACTIVE")) {
            throw new IllegalStateException("Already an active member of this circle");
        }

        // If circle does not require approval, join directly
        if (!circle.getApprovalRequired()) {
            KittyPartyMember newMember = KittyPartyMember.builder()
                    .circle(circle)
                    .user(user)
                    .community(user.getCommunity())
                    .roleCode("MEMBER")
                    .membershipStatus("ACTIVE")
                    .approvedBy(user)
                    .approvedAt(LocalDateTime.now())
                    .build();
            memberRepository.save(newMember);
            return;
        }

        KittyPartyJoinRequest request = joinRequestRepository.findByCircleIdAndUserId(circleId, user.getId())
                .orElseGet(() -> KittyPartyJoinRequest.builder()
                        .circle(circle)
                        .user(user)
                        .build());

        request.setRequestMessage(payload != null ? payload.getRequestMessage() : null);
        request.setStatus("PENDING");
        joinRequestRepository.save(request);
        log.info("User {} requested to join circle {}", user.getId(), circleId);
    }

    @Transactional
    public void approveJoinRequest(Long circleId, Long joinRequestId, AppUser reviewer) {
        authorizationService.assertAdminOrOwner(circleId, reviewer.getId());

        KittyPartyJoinRequest request = joinRequestRepository.findById(joinRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Join request not found"));

        if (!request.getCircle().getId().equals(circleId)) {
            throw new IllegalArgumentException("Join request mismatch with circle");
        }

        request.setStatus("APPROVED");
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        joinRequestRepository.save(request);

        // Add member
        KittyPartyMember member = memberRepository.findByCircleIdAndUserId(circleId, request.getUser().getId())
                .orElseGet(() -> KittyPartyMember.builder()
                        .circle(request.getCircle())
                        .user(request.getUser())
                        .community(request.getUser().getCommunity())
                        .build());

        member.setMembershipStatus("ACTIVE");
        member.setRoleCode("MEMBER");
        member.setApprovedBy(reviewer);
        member.setApprovedAt(LocalDateTime.now());
        memberRepository.save(member);
    }

    @Transactional
    public String inviteMember(Long circleId, AppUser inviter, InviteRequest request) {
        authorizationService.assertAdminOrOwner(circleId, inviter.getId());

        KittyPartyCircle circle = circleRepository.findByIdAndDeletedAtIsNull(circleId)
                .orElseThrow(() -> new IllegalArgumentException("Circle not found"));

        boolean isExternal = Boolean.TRUE.equals(request.getIsExternal());
        if (isExternal) {
            if (!Boolean.TRUE.equals(circle.getAllowExternalMembers())) {
                throw new IllegalStateException("This circle does not allow external guest members");
            }
            long activeExternalCount = memberRepository.countByCircleIdAndIsExternalTrueAndMembershipStatus(circleId, "ACTIVE");
            if (circle.getMaxExternalMembers() != null && activeExternalCount >= circle.getMaxExternalMembers()) {
                throw new IllegalStateException("Maximum external guest members (" + circle.getMaxExternalMembers() + ") reached for this circle");
            }
        }

        AppUser invitedUser = null;
        if (request.getInvitedUserId() != null) {
            invitedUser = userRepository.findById(request.getInvitedUserId()).orElse(null);
        } else if (request.getInvitedPhone() != null && !request.getInvitedPhone().isBlank()) {
            invitedUser = userRepository.findByPhone(request.getInvitedPhone().trim()).orElse(null);
            if (invitedUser == null && isExternal) {
                // Provision guest user for external friend
                invitedUser = AppUser.builder()
                        .fullName(request.getInvitedFullName() != null ? request.getInvitedFullName().trim() : "Guest Member")
                        .phone(request.getInvitedPhone().trim())
                        .community(circle.getCommunity())
                        .residentType("GUEST")
                        .isActive(true)
                        .build();
                invitedUser = userRepository.save(invitedUser);
            }
        }

        if (invitedUser != null && isExternal) {
            final AppUser targetUser = invitedUser;
            KittyPartyMember externalMember = memberRepository.findByCircleIdAndUserId(circleId, targetUser.getId())
                    .orElseGet(() -> KittyPartyMember.builder()
                            .circle(circle)
                            .user(targetUser)
                            .community(circle.getCommunity())
                            .build());
            externalMember.setIsExternal(true);
            externalMember.setSponsorUser(inviter);
            externalMember.setExternalSocietyName(request.getExternalSocietyName());
            externalMember.setMembershipStatus("ACTIVE");
            externalMember.setRoleCode("MEMBER");
            externalMember.setApprovedBy(inviter);
            externalMember.setApprovedAt(LocalDateTime.now());
            externalMember.setJoinedAt(LocalDateTime.now());
            memberRepository.save(externalMember);
            log.info("Directly enrolled external member {} sponsored by {} in circle {}", invitedUser.getId(), inviter.getId(), circleId);
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        KittyPartyInvitation invitation = KittyPartyInvitation.builder()
                .circle(circle)
                .invitedUser(invitedUser)
                .invitedPhone(request.getInvitedPhone())
                .inviteTokenHash(token)
                .status("PENDING")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdBy(inviter)
                .build();

        invitationRepository.save(invitation);
        log.info("Circle {} invitation created by user {}", circleId, inviter.getId());
        return token;
    }

    @Transactional
    public void acceptInvitation(String token, AppUser user) {
        KittyPartyInvitation invitation = invitationRepository.findByInviteTokenHash(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invitation token"));

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus("EXPIRED");
            invitationRepository.save(invitation);
            throw new IllegalStateException("Invitation token has expired");
        }

        if (!"PENDING".equals(invitation.getStatus())) {
            throw new IllegalStateException("Invitation is no longer pending");
        }

        invitation.setStatus("ACCEPTED");
        invitationRepository.save(invitation);

        KittyPartyMember member = memberRepository.findByCircleIdAndUserId(invitation.getCircle().getId(), user.getId())
                .orElseGet(() -> KittyPartyMember.builder()
                        .circle(invitation.getCircle())
                        .user(user)
                        .community(user.getCommunity())
                        .build());

        member.setMembershipStatus("ACTIVE");
        member.setRoleCode("MEMBER");
        member.setApprovedBy(invitation.getCreatedBy());
        member.setApprovedAt(LocalDateTime.now());
        memberRepository.save(member);
    }

    // Role assertions delegated to KittyPartyAuthorizationService

    private CircleResponse mapToResponse(KittyPartyCircle c, String currentUserRole, Long memberCount) {
        long externalCount = memberRepository.countByCircleIdAndIsExternalTrueAndMembershipStatus(c.getId(), "ACTIVE");
        return CircleResponse.builder()
                .id(c.getId())
                .communityId(c.getCommunity().getId())
                .circleName(c.getCircleName())
                .description(c.getDescription())
                .categoryCode(c.getCategoryCode())
                .privacyType(c.getPrivacyType())
                .approvalRequired(c.getApprovalRequired())
                .maxMembers(c.getMaxMembers())
                .allowExternalMembers(c.getAllowExternalMembers())
                .maxExternalMembers(c.getMaxExternalMembers())
                .externalMembersCount(externalCount)
                .currentMembersCount(memberCount)
                .status(c.getStatus())
                .createdById(c.getCreatedBy().getId())
                .createdByName(c.getCreatedBy().getFullName())
                .createdAt(c.getCreatedAt())
                .currentUserRole(currentUserRole)
                .build();
    }
}
