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
                .categoryCode(request.getCategoryCode() != null ? request.getCategoryCode().toUpperCase() : "KITTY")
                .privacyType(request.getPrivacyType() != null ? request.getPrivacyType().toUpperCase() : "PRIVATE")
                .approvalRequired(request.getApprovalRequired() == null || request.getApprovalRequired())
                .maxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : 20)
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
                .map(m -> MemberResponse.builder()
                        .memberId(m.getId())
                        .userId(m.getUser().getId())
                        .fullName(m.getUser().getFullName())
                        .phone(m.getUser().getPhone())
                        .roleCode(m.getRoleCode())
                        .membershipStatus(m.getMembershipStatus())
                        .joinedAt(m.getJoinedAt())
                        .build())
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
        assertAdminOrOwner(circleId, reviewer.getId());

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
        assertAdminOrOwner(circleId, inviter.getId());

        KittyPartyCircle circle = circleRepository.findByIdAndDeletedAtIsNull(circleId)
                .orElseThrow(() -> new IllegalArgumentException("Circle not found"));

        AppUser invitedUser = null;
        if (request.getInvitedUserId() != null) {
            invitedUser = userRepository.findById(request.getInvitedUserId()).orElse(null);
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

    private void assertAdminOrOwner(Long circleId, Long userId) {
        KittyPartyMember member = memberRepository.findByCircleIdAndUserId(circleId, userId)
                .orElseThrow(() -> new SecurityException("User is not a member of this circle"));
        if (!"OWNER".equals(member.getRoleCode()) && !"ADMIN".equals(member.getRoleCode())) {
            throw new SecurityException("Requires circle Admin or Owner role");
        }
    }

    private CircleResponse mapToResponse(KittyPartyCircle c, String currentUserRole, Long memberCount) {
        return CircleResponse.builder()
                .id(c.getId())
                .communityId(c.getCommunity().getId())
                .circleName(c.getCircleName())
                .description(c.getDescription())
                .categoryCode(c.getCategoryCode())
                .privacyType(c.getPrivacyType())
                .approvalRequired(c.getApprovalRequired())
                .maxMembers(c.getMaxMembers())
                .currentMembersCount(memberCount)
                .status(c.getStatus())
                .createdById(c.getCreatedBy().getId())
                .createdByName(c.getCreatedBy().getFullName())
                .createdAt(c.getCreatedAt())
                .currentUserRole(currentUserRole)
                .build();
    }
}
