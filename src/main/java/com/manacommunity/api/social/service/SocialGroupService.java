package com.manacommunity.api.social.service;

import com.manacommunity.api.social.entity.*;
import com.manacommunity.api.social.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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

    public SocialGroup createGroup(Long communityId, Long createdBy, String name, String description,
                                    String groupType, boolean isPrivate, int maxMembers) {
        SocialGroup group = SocialGroup.builder()
                .communityId(communityId)
                .createdBy(createdBy)
                .name(name)
                .description(description)
                .groupType(groupType != null ? groupType : "INTEREST")
                .isPrivate(isPrivate)
                .maxMembers(maxMembers > 0 ? maxMembers : 50)
                .build();
        group = groupRepo.save(group);
        // Creator becomes ADMIN member
        memberRepo.save(SocialGroupMember.builder()
                .groupId(group.getId())
                .userId(createdBy)
                .role("ADMIN")
                .build());
        return group;
    }

    @Transactional(readOnly = true)
    public Page<SocialGroup> getGroupsByCommunity(Long communityId, String groupType, Pageable pageable) {
        if (groupType != null && !groupType.isBlank()) {
            return groupRepo.findByCommunityIdAndGroupTypeAndIsActiveTrue(communityId, groupType, pageable);
        }
        return groupRepo.findByCommunityIdAndIsActiveTrue(communityId, pageable);
    }

    @Transactional(readOnly = true)
    public List<SocialGroup> getMyGroups(Long userId) {
        List<Long> groupIds = memberRepo.findGroupIdsByUserId(userId);
        if (groupIds.isEmpty()) return List.of();
        return groupRepo.findAllById(groupIds).stream()
                .filter(SocialGroup::isActive).toList();
    }

    public void joinGroup(Long groupId, Long userId) {
        SocialGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        if (group.isPrivate()) {
            throw new SecurityException("This group requires an invitation to join");
        }
        if (!memberRepo.existsByGroupIdAndUserId(groupId, userId)) {
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
        GroupMessage msg = GroupMessage.builder()
                .groupId(groupId)
                .senderId(senderId)
                .content(content)
                .messageType(messageType != null ? messageType : "TEXT")
                .build();
        msg = messageRepo.save(msg);
        ws.convertAndSend("/topic/groups/" + groupId + "/messages",
                Map.of("id", msg.getId(), "groupId", groupId, "senderId", senderId,
                        "content", content, "type", msg.getMessageType(),
                        "sentAt", msg.getSentAt().toString()));
        return msg;
    }

    @Transactional(readOnly = true)
    public Page<GroupMessage> getMessages(Long groupId, Long requesterId, Pageable pageable) {
        if (!memberRepo.existsByGroupIdAndUserId(groupId, requesterId)) {
            throw new SecurityException("Not a group member");
        }
        return messageRepo.findByGroupIdAndDeletedAtIsNullOrderBySentAtDesc(groupId, pageable);
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
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
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
        return eventRepo.findByGroupIdAndEventDateAfterOrderByEventDateAsc(groupId, OffsetDateTime.now());
    }

    public GroupExpense addExpense(Long groupId, Long paidBy, BigDecimal amount, String description) {
        if (!memberRepo.existsByGroupIdAndUserId(groupId, paidBy)) {
            throw new SecurityException("Not a group member");
        }
        List<SocialGroupMember> members = memberRepo.findByGroupId(groupId);
        GroupExpense expense = expenseRepo.save(GroupExpense.builder()
                .groupId(groupId).paidBy(paidBy).amount(amount)
                .description(description).splitType("EQUAL")
                .build());
        BigDecimal share = amount.divide(BigDecimal.valueOf(members.size()), 2, RoundingMode.HALF_UP);
        members.forEach(m -> splitRepo.save(GroupExpenseSplit.builder()
                .expenseId(expense.getId())
                .memberId(m.getUserId())
                .amountOwed(share)
                .build()));
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
        return expenseRepo.findByGroupIdOrderByCreatedAtDesc(groupId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getMyBalance(Long groupId, Long userId) {
        BigDecimal owed = splitRepo.sumUnsettledOwedByUser(groupId, userId);
        BigDecimal owedToMe = splitRepo.sumUnsettledOwedToUser(groupId, userId);
        return owedToMe.subtract(owed);
    }
}
