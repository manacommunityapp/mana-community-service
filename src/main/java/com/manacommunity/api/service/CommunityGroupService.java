package com.manacommunity.api.service;

import com.manacommunity.api.dto.GroupRequest;
import com.manacommunity.api.dto.GroupResponse;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.CommunityGroup;
import com.manacommunity.api.model.GroupMembership;
import com.manacommunity.api.repository.CommunityGroupRepository;
import com.manacommunity.api.repository.GroupMembershipRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommunityGroupService {

    private final CommunityGroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public Page<GroupResponse> getGroups(AppUser currentUser, String category, String search, int page, int size) {
        if (currentUser.getCommunity() == null) {
            throw new InvalidInputException("User is not associated with any community.");
        }
        Pageable pageable = PageRequest.of(page, size);
        Long communityId = currentUser.getCommunity().getId();

        Page<CommunityGroup> groups;
        if (search != null && !search.isBlank()) {
            groups = groupRepository.searchGroups(communityId, search, pageable);
        } else if (category != null && !category.isBlank()) {
            groups = groupRepository.findByCategory(communityId, category, pageable);
        } else {
            groups = groupRepository.findByCommunityIdAndActiveTrue(communityId, pageable);
        }

        return groups.map(g -> toGroupResponse(g, currentUser.getId()));
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(AppUser currentUser, Long groupId) {
        CommunityGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));
        return toGroupResponse(group, currentUser.getId());
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getMyGroups(AppUser currentUser) {
        List<GroupMembership> memberships = membershipRepository.findByUserIdAndStatus(currentUser.getId(), "ACTIVE");
        return memberships.stream()
                .map(m -> toGroupResponse(m.getGroup(), currentUser.getId()))
                .toList();
    }

    @Transactional
    public GroupResponse createGroup(AppUser currentUser, GroupRequest request) {
        if (currentUser.getCommunity() == null) {
            throw new InvalidInputException("User is not associated with any community.");
        }

        String slug = request.name().toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");

        CommunityGroup group = CommunityGroup.builder()
                .community(currentUser.getCommunity())
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .coverImageUrl(request.coverImageUrl())
                .iconUrl(request.iconUrl())
                .groupType(request.groupType() != null ? request.groupType() : "PUBLIC")
                .category(request.category())
                .createdBy(currentUser)
                .requiresApproval(request.requiresApproval() != null && request.requiresApproval())
                .allowMemberPosts(request.allowMemberPosts() == null || request.allowMemberPosts())
                .rules(request.rules())
                .tags(request.tags())
                .build();

        CommunityGroup saved = groupRepository.save(group);

        GroupMembership membership = GroupMembership.builder()
                .group(saved)
                .user(currentUser)
                .role("ADMIN")
                .status("ACTIVE")
                .build();
        membershipRepository.save(membership);
        saved.setMemberCount(1);
        groupRepository.save(saved);

        return toGroupResponse(saved, currentUser.getId());
    }

    @Transactional
    public GroupResponse updateGroup(AppUser currentUser, Long groupId, GroupRequest request) {
        CommunityGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));

        Optional<GroupMembership> membership = membershipRepository.findByGroupIdAndUserId(groupId, currentUser.getId());
        boolean isGroupAdmin = membership.isPresent() && "ADMIN".equals(membership.get().getRole());
        if (!isGroupAdmin && !isAdminRole(currentUser.getRole())) {
            throw new UnauthorizedActionException("update group " + groupId);
        }

        if (request.name() != null) group.setName(request.name());
        if (request.description() != null) group.setDescription(request.description());
        if (request.coverImageUrl() != null) group.setCoverImageUrl(request.coverImageUrl());
        if (request.iconUrl() != null) group.setIconUrl(request.iconUrl());
        if (request.groupType() != null) group.setGroupType(request.groupType());
        if (request.category() != null) group.setCategory(request.category());
        if (request.requiresApproval() != null) group.setRequiresApproval(request.requiresApproval());
        if (request.allowMemberPosts() != null) group.setAllowMemberPosts(request.allowMemberPosts());
        if (request.rules() != null) group.setRules(request.rules());
        if (request.tags() != null) group.setTags(request.tags());

        groupRepository.save(group);
        return toGroupResponse(group, currentUser.getId());
    }

    @Transactional
    public GroupResponse joinGroup(AppUser currentUser, Long groupId) {
        CommunityGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));

        if (membershipRepository.existsByGroupIdAndUserId(groupId, currentUser.getId())) {
            throw new InvalidInputException("Already a member of this group.");
        }

        String status = group.isRequiresApproval() ? "PENDING" : "ACTIVE";
        GroupMembership membership = GroupMembership.builder()
                .group(group)
                .user(currentUser)
                .role("MEMBER")
                .status(status)
                .build();
        membershipRepository.save(membership);

        if ("ACTIVE".equals(status)) {
            group.setMemberCount(group.getMemberCount() + 1);
            groupRepository.save(group);
        }

        return toGroupResponse(group, currentUser.getId());
    }

    @Transactional
    public void leaveGroup(AppUser currentUser, Long groupId) {
        CommunityGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));

        Optional<GroupMembership> membership = membershipRepository.findByGroupIdAndUserId(groupId, currentUser.getId());
        if (membership.isEmpty()) {
            throw new InvalidInputException("Not a member of this group.");
        }

        membershipRepository.delete(membership.get());
        if ("ACTIVE".equals(membership.get().getStatus())) {
            group.setMemberCount(Math.max(0, group.getMemberCount() - 1));
            groupRepository.save(group);
        }
    }

    @Transactional(readOnly = true)
    public Page<GroupMemberResponse> getGroupMembers(Long groupId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GroupMembership> members = membershipRepository.findByGroupIdAndStatus(groupId, "ACTIVE", pageable);
        return members.map(m -> new GroupMemberResponse(
                m.getUser().getId(),
                m.getUser().getFullName(),
                m.getUser().getProfilePicUrl(),
                m.getRole(),
                m.getJoinedAt()
        ));
    }

    private GroupResponse toGroupResponse(CommunityGroup group, Long currentUserId) {
        Optional<GroupMembership> membership = membershipRepository.findByGroupIdAndUserId(group.getId(), currentUserId);
        boolean isMember = membership.isPresent() && "ACTIVE".equals(membership.get().getStatus());
        String memberRole = membership.map(GroupMembership::getRole).orElse(null);

        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getSlug(),
                group.getDescription(),
                group.getCoverImageUrl(),
                group.getIconUrl(),
                group.getGroupType(),
                group.getCategory(),
                group.getMemberCount(),
                group.getPostCount(),
                group.isRequiresApproval(),
                group.isAllowMemberPosts(),
                group.getRules(),
                group.getTags(),
                group.getCreatedBy().getId(),
                group.getCreatedBy().getFullName(),
                isMember,
                memberRole,
                group.getCreatedAt()
        );
    }

    private boolean isAdminRole(String role) {
        return role != null && java.util.Set.of("ADMIN", "SUPER_ADMIN", "COMMUNITY_ADMIN").contains(role.toUpperCase());
    }

    public record GroupMemberResponse(
        Long userId,
        String fullName,
        String profilePicUrl,
        String role,
        java.time.LocalDateTime joinedAt
    ) {}
}
