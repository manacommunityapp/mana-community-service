package com.manacommunity.api.social.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.social.entity.*;
import com.manacommunity.api.social.repository.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class SocialGroupService {

    private final SocialGroupRepository groupRepo;
    private final SocialGroupMemberRepository memberRepo;
    private final GroupMessageRepository messageRepo;
    private final GroupEventRepository eventRepo;
    private final GroupEventRsvpRepository rsvpRepo;
    private final GroupExpenseRepository expenseRepo;
    private final GroupExpenseSplitRepository splitRepo;
    private final SimpMessagingTemplate ws;

    @Autowired(required = false)
    private AppUserRepository userRepo;

    private String resolveUserName(Long userId) {
        if (userId == null) return "Member";
        if (userRepo != null) {
            return userRepo.findById(userId)
                    .map(u -> (u.getName() != null && !u.getName().isBlank()) ? u.getName() : "Member #" + userId)
                    .orElse("Member #" + userId);
        }
        return "Member #" + userId;
    }

    private String resolveUserAvatar(Long userId) {
        if (userId == null || userRepo == null) return null;
        return userRepo.findById(userId)
                .map(AppUser::getProfilePicUrl)
                .orElse(null);
    }

    public SocialGroup createGroup(Long communityId, Long createdBy, String name, String description,
                                    String groupType, boolean isPrivate, int maxMembers) {
        SocialGroup group = SocialGroup.builder()
                .communityId(communityId)
                .createdBy(createdBy)
                .name(name)
                .description(description)
                .groupType(groupType != null && !groupType.isBlank() ? groupType : "INTEREST")
                .isPrivate(isPrivate)
                .maxMembers(maxMembers > 0 ? maxMembers : 50)
                .memberCount(1)
                .isActive(true)
                .build();
        group = groupRepo.save(group);
        // Creator becomes ADMIN member
        memberRepo.save(SocialGroupMember.builder()
                .groupId(group.getId())
                .userId(createdBy)
                .role("ADMIN")
                .build());
        group.setIsMember(true);
        return group;
    }

    @Transactional(readOnly = true)
    public SocialGroup getGroupById(Long groupId, Long currentUserId) {
        SocialGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Social Group", groupId));
        boolean member = currentUserId != null && memberRepo.existsByGroupIdAndUserId(groupId, currentUserId);
        group.setIsMember(member);
        return group;
    }

    @Transactional(readOnly = true)
    public Page<SocialGroup> getGroupsByCommunity(Long communityId, String groupType, Pageable pageable) {
        return getGroupsByCommunity(communityId, groupType, pageable, null);
    }

    @Transactional(readOnly = true)
    public Page<SocialGroup> getGroupsByCommunity(Long communityId, String groupType, Pageable pageable, Long currentUserId) {
        Page<SocialGroup> page;
        if (groupType != null && !groupType.isBlank()) {
            page = groupRepo.findByCommunityIdAndGroupTypeAndIsActiveTrue(communityId, groupType, pageable);
        } else {
            page = groupRepo.findByCommunityIdAndIsActiveTrue(communityId, pageable);
        }
        Set<Long> myGroupIds = currentUserId != null
                ? new HashSet<>(memberRepo.findGroupIdsByUserId(currentUserId))
                : Set.of();
        page.getContent().forEach(g -> g.setIsMember(myGroupIds.contains(g.getId())));
        return page;
    }

    @Transactional(readOnly = true)
    public List<SocialGroup> getMyGroups(Long userId) {
        if (userId == null) return List.of();
        List<Long> groupIds = memberRepo.findGroupIdsByUserId(userId);
        if (groupIds.isEmpty()) return List.of();
        return groupRepo.findAllById(groupIds).stream()
                .filter(SocialGroup::isActive)
                .peek(g -> g.setIsMember(true))
                .toList();
    }

    public void joinGroup(Long groupId, Long userId) {
        SocialGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Social Group", groupId));
        if (group.isPrivate()) {
            throw new SecurityException("This group requires an invitation to join");
        }
        if (!memberRepo.existsByGroupIdAndUserId(groupId, userId)) {
            if (group.getMaxMembers() > 0 && group.getMemberCount() >= group.getMaxMembers()) {
                throw new IllegalStateException("This group has reached its maximum member capacity");
            }
            memberRepo.save(SocialGroupMember.builder()
                    .groupId(groupId).userId(userId).role("MEMBER").build());
            group.setMemberCount(group.getMemberCount() + 1);
            groupRepo.save(group);
        }
    }

    public void leaveGroup(Long groupId, Long userId) {
        memberRepo.findByGroupIdAndUserId(groupId, userId).ifPresent(m -> {
            memberRepo.delete(m);
            groupRepo.findById(groupId).ifPresent(g -> {
                g.setMemberCount(Math.max(0, g.getMemberCount() - 1));
                groupRepo.save(g);
            });
        });
    }

    public void inviteMember(Long groupId, Long adminId, Long inviteeId) {
        SocialGroupMember admin = memberRepo.findByGroupIdAndUserId(groupId, adminId)
                .orElseThrow(() -> new SecurityException("Not a group member"));
        if (!"ADMIN".equals(admin.getRole())) {
            throw new SecurityException("Only admins can invite members");
        }
        if (!memberRepo.existsByGroupIdAndUserId(groupId, inviteeId)) {
            memberRepo.save(SocialGroupMember.builder()
                    .groupId(groupId).userId(inviteeId).role("MEMBER").build());
            groupRepo.findById(groupId).ifPresent(g -> {
                g.setMemberCount(g.getMemberCount() + 1);
                groupRepo.save(g);
            });
        }
    }

    public GroupMessage sendMessage(Long groupId, Long senderId, String content, String messageType) {
        if (!memberRepo.existsByGroupIdAndUserId(groupId, senderId)) {
            throw new SecurityException("Not a group member");
        }
        OffsetDateTime now = OffsetDateTime.now();
        GroupMessage msg = GroupMessage.builder()
                .groupId(groupId)
                .senderId(senderId)
                .content(content)
                .messageType(messageType != null ? messageType : "TEXT")
                .sentAt(now)
                .build();
        msg = messageRepo.save(msg);
        if (msg.getSentAt() == null) {
            msg.setSentAt(now);
        }
        String senderName = resolveUserName(senderId);
        String avatarUrl = resolveUserAvatar(senderId);
        msg.setSenderName(senderName);
        msg.setSenderAvatarUrl(avatarUrl);

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", msg.getId());
        payload.put("groupId", groupId);
        payload.put("senderId", senderId);
        payload.put("senderName", senderName);
        if (avatarUrl != null) payload.put("senderAvatarUrl", avatarUrl);
        payload.put("content", content);
        payload.put("type", msg.getMessageType());
        payload.put("sentAt", msg.getSentAt().toString());
        ws.convertAndSend("/topic/groups/" + groupId + "/messages", payload);
        return msg;
    }

    @Transactional(readOnly = true)
    public Page<GroupMessage> getMessages(Long groupId, Long requesterId, Pageable pageable) {
        SocialGroup group = groupRepo.findById(groupId).orElse(null);
        boolean isPrivate = group == null || group.isPrivate();
        if (isPrivate && (requesterId == null || !memberRepo.existsByGroupIdAndUserId(groupId, requesterId))) {
            throw new SecurityException("Not a group member");
        }
        Page<GroupMessage> page = messageRepo.findByGroupIdAndDeletedAtIsNullOrderBySentAtDesc(groupId, pageable);
        page.getContent().forEach(m -> {
            m.setSenderName(resolveUserName(m.getSenderId()));
            m.setSenderAvatarUrl(resolveUserAvatar(m.getSenderId()));
        });
        return page;
    }

    public GroupEvent createGroupEvent(Long groupId, Long createdBy, String title, String description,
                                        OffsetDateTime eventDate, String location, OffsetDateTime rsvpDeadline) {
        if (!memberRepo.existsByGroupIdAndUserId(groupId, createdBy)) {
            throw new SecurityException("Not a group member");
        }
        return eventRepo.save(GroupEvent.builder()
                .groupId(groupId).createdBy(createdBy).title(title).description(description)
                .eventDate(eventDate).location(location).rsvpDeadline(rsvpDeadline)
                .build());
    }

    public void rsvpEvent(Long eventId, Long userId, String response) {
        GroupEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Group Event", eventId));
        if (!memberRepo.existsByGroupIdAndUserId(event.getGroupId(), userId)) {
            throw new SecurityException("Not a group member");
        }
        GroupEventRsvp rsvp = rsvpRepo.findByEventIdAndUserId(eventId, userId)
                .orElseGet(() -> GroupEventRsvp.builder().eventId(eventId).userId(userId).build());
        rsvp.setResponse(response);
        rsvpRepo.save(rsvp);
    }

    @Transactional(readOnly = true)
    public List<GroupEvent> getUpcomingEvents(Long groupId) {
        return getUpcomingEvents(groupId, null);
    }

    @Transactional(readOnly = true)
    public List<GroupEvent> getUpcomingEvents(Long groupId, Long currentUserId) {
        List<GroupEvent> events = eventRepo.findByGroupIdAndEventDateAfterOrderByEventDateAsc(groupId, OffsetDateTime.now());
        if (currentUserId != null) {
            events.forEach(ev -> rsvpRepo.findByEventIdAndUserId(ev.getId(), currentUserId)
                    .ifPresent(r -> ev.setRsvpStatus(r.getResponse())));
        }
        return events;
    }

    public GroupExpense addExpense(Long groupId, Long paidBy, BigDecimal amount, String description) {
        if (!memberRepo.existsByGroupIdAndUserId(groupId, paidBy)) {
            throw new SecurityException("Not a group member");
        }
        List<SocialGroupMember> members = memberRepo.findByGroupId(groupId);
        int memberCount = Math.max(1, members.size());
        GroupExpense expense = expenseRepo.save(GroupExpense.builder()
                .groupId(groupId).paidBy(paidBy).amount(amount)
                .description(description).splitType("EQUAL")
                .build());
        BigDecimal share = amount.divide(BigDecimal.valueOf(memberCount), 2, RoundingMode.HALF_UP);
        OffsetDateTime now = OffsetDateTime.now();
        members.forEach(m -> {
            boolean isPayer = m.getUserId().equals(paidBy);
            splitRepo.save(GroupExpenseSplit.builder()
                    .expenseId(expense.getId())
                    .memberId(m.getUserId())
                    .amountOwed(share)
                    .isSettled(isPayer)
                    .settledAt(isPayer ? now : null)
                    .build());
        });
        expense.setPaidByName(resolveUserName(paidBy));
        expense.setSplitBetween(memberCount);
        expense.setMyShare(share);
        expense.setMyBalance(amount.subtract(share));
        return expense;
    }

    public void settleExpense(Long expenseId, Long memberId) {
        splitRepo.findByExpenseIdAndMemberId(expenseId, memberId).ifPresent(s -> {
            s.setSettled(true);
            s.setSettledAt(OffsetDateTime.now());
            splitRepo.save(s);
        });
    }

    @Transactional(readOnly = true)
    public List<GroupExpense> listExpenses(Long groupId) {
        return listExpenses(groupId, null);
    }

    @Transactional(readOnly = true)
    public List<GroupExpense> listExpenses(Long groupId, Long currentUserId) {
        List<GroupExpense> expenses = expenseRepo.findByGroupIdOrderByCreatedAtDesc(groupId);
        for (GroupExpense e : expenses) {
            e.setPaidByName(resolveUserName(e.getPaidBy()));
            List<GroupExpenseSplit> splits = splitRepo.findByExpenseId(e.getId());
            int count = Math.max(1, splits.size());
            e.setSplitBetween(count);
            BigDecimal defaultShare = e.getAmount() != null
                    ? e.getAmount().divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            if (currentUserId == null) {
                e.setMyShare(defaultShare);
                e.setMyBalance(BigDecimal.ZERO);
            } else if (currentUserId.equals(e.getPaidBy())) {
                BigDecimal myShare = splits.stream()
                        .filter(s -> currentUserId.equals(s.getMemberId()))
                        .map(GroupExpenseSplit::getAmountOwed)
                        .findFirst()
                        .orElse(defaultShare);
                BigDecimal unsettledOwedToMe = splits.stream()
                        .filter(s -> !currentUserId.equals(s.getMemberId()) && !s.isSettled())
                        .map(GroupExpenseSplit::getAmountOwed)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                e.setMyShare(myShare);
                e.setMyBalance(unsettledOwedToMe);
            } else {
                GroupExpenseSplit mySplit = splits.stream()
                        .filter(s -> currentUserId.equals(s.getMemberId()))
                        .findFirst()
                        .orElse(null);
                BigDecimal myShare = mySplit != null ? mySplit.getAmountOwed() : defaultShare;
                BigDecimal myBalance = (mySplit != null && !mySplit.isSettled())
                        ? mySplit.getAmountOwed().negate()
                        : BigDecimal.ZERO;
                e.setMyShare(myShare);
                e.setMyBalance(myBalance);
            }
        }
        return expenses;
    }

    @Transactional(readOnly = true)
    public BigDecimal getMyBalance(Long groupId, Long userId) {
        BigDecimal owed = splitRepo.sumUnsettledOwedByUser(groupId, userId);
        BigDecimal owedToMe = splitRepo.sumUnsettledOwedToUser(groupId, userId);
        return (owedToMe != null ? owedToMe : BigDecimal.ZERO)
                .subtract(owed != null ? owed : BigDecimal.ZERO);
    }
}
