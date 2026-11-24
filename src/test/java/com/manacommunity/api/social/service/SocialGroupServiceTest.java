package com.manacommunity.api.social.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.social.entity.*;
import com.manacommunity.api.social.repository.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Social Circles / Groups Service Test Suite")
class SocialGroupServiceTest {

    @Mock private SocialGroupRepository groupRepo;
    @Mock private SocialGroupMemberRepository memberRepo;
    @Mock private GroupMessageRepository messageRepo;
    @Mock private GroupEventRepository eventRepo;
    @Mock private GroupEventRsvpRepository rsvpRepo;
    @Mock private GroupExpenseRepository expenseRepo;
    @Mock private GroupExpenseSplitRepository splitRepo;
    @Mock private SimpMessagingTemplate ws;
    @Mock private AppUserRepository userRepo;

    @InjectMocks
    private SocialGroupService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "userRepo", userRepo);
    }

    @Test
    @DisplayName("createGroup assigns creator as ADMIN and marks isMember true")
    void createGroup_assignsCreatorAsAdminAndSetsIsMember() {
        when(groupRepo.save(any(SocialGroup.class))).thenAnswer(inv -> {
            SocialGroup g = inv.getArgument(0);
            g.setId(10L);
            return g;
        });

        SocialGroup created = service.createGroup(1L, 42L, "Morning Runners", "Daily 6am run", "FITNESS", false, 30);

        assertEquals(10L, created.getId());
        assertEquals("Morning Runners", created.getName());
        assertEquals(Boolean.TRUE, created.getIsMember());
        verify(memberRepo).save(argThat(m ->
                m.getGroupId().equals(10L) && m.getUserId().equals(42L) && "ADMIN".equals(m.getRole())));
    }

    @Test
    @DisplayName("getGroupsByCommunity marks isMember accurately for current user")
    void getGroupsByCommunity_populatesIsMemberFlag() {
        SocialGroup g1 = SocialGroup.builder().id(1L).communityId(1L).name("Book Club").build();
        SocialGroup g2 = SocialGroup.builder().id(2L).communityId(1L).name("Chess Club").build();
        when(groupRepo.findByCommunityIdAndIsActiveTrue(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(g1, g2)));
        when(memberRepo.findGroupIdsByUserId(99L)).thenReturn(List.of(1L));

        Page<SocialGroup> page = service.getGroupsByCommunity(1L, null, PageRequest.of(0, 20), 99L);

        assertEquals(2, page.getContent().size());
        assertEquals(Boolean.TRUE, page.getContent().get(0).getIsMember());
        assertEquals(Boolean.FALSE, page.getContent().get(1).getIsMember());
    }

    @Test
    @DisplayName("getGroupById throws ResourceNotFoundException when missing")
    void getGroupById_notFound_throwsResourceNotFoundException() {
        when(groupRepo.findById(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getGroupById(404L, 1L));
    }

    @Test
    @DisplayName("sendMessage enriches senderName and broadcasts to WebSocket")
    void sendMessage_enrichesSenderNameAndBroadcasts() {
        when(memberRepo.existsByGroupIdAndUserId(5L, 42L)).thenReturn(true);
        AppUser sender = new AppUser();
        sender.setId(42L);
        sender.setFullName("Aarav Sharma");
        sender.setProfilePicUrl("https://cdn.example.com/aarav.png");
        when(userRepo.findById(42L)).thenReturn(Optional.of(sender));
        when(messageRepo.save(any(GroupMessage.class))).thenAnswer(inv -> {
            GroupMessage m = inv.getArgument(0);
            m.setId(101L);
            return m;
        });

        GroupMessage msg = service.sendMessage(5L, 42L, "Hello neighbors!", "TEXT");

        assertEquals(101L, msg.getId());
        assertEquals("Aarav Sharma", msg.getSenderName());
        assertEquals("https://cdn.example.com/aarav.png", msg.getSenderAvatarUrl());
        verify(ws).convertAndSend(eq("/topic/groups/5/messages"), anyMap());
    }

    @Test
    @DisplayName("addExpense marks payer's split as settled and calculates accurate share & balance")
    void addExpense_marksPayerSplitSettledAndComputesBalance() {
        when(memberRepo.existsByGroupIdAndUserId(5L, 10L)).thenReturn(true);
        SocialGroupMember m1 = SocialGroupMember.builder().groupId(5L).userId(10L).role("ADMIN").build();
        SocialGroupMember m2 = SocialGroupMember.builder().groupId(5L).userId(20L).role("MEMBER").build();
        when(memberRepo.findByGroupId(5L)).thenReturn(List.of(m1, m2));
        when(expenseRepo.save(any(GroupExpense.class))).thenAnswer(inv -> {
            GroupExpense e = inv.getArgument(0);
            e.setId(77L);
            return e;
        });
        AppUser payer = new AppUser();
        payer.setId(10L);
        payer.setFullName("Priya Nair");
        when(userRepo.findById(10L)).thenReturn(Optional.of(payer));

        GroupExpense saved = service.addExpense(5L, 10L, new BigDecimal("100.00"), "Shuttlecocks");

        assertEquals("Priya Nair", saved.getPaidByName());
        assertEquals(2, saved.getSplitBetween());
        assertEquals(new BigDecimal("50.00"), saved.getMyShare());
        assertEquals(new BigDecimal("50.00"), saved.getMyBalance());

        ArgumentCaptor<GroupExpenseSplit> splitCaptor = ArgumentCaptor.forClass(GroupExpenseSplit.class);
        verify(splitRepo, times(2)).save(splitCaptor.capture());
        List<GroupExpenseSplit> splits = splitCaptor.getAllValues();

        GroupExpenseSplit payerSplit = splits.stream().filter(s -> s.getMemberId().equals(10L)).findFirst().orElseThrow();
        GroupExpenseSplit otherSplit = splits.stream().filter(s -> s.getMemberId().equals(20L)).findFirst().orElseThrow();

        assertTrue(payerSplit.isSettled(), "Payer's own share must be marked settled at creation");
        assertNotNull(payerSplit.getSettledAt());
        assertFalse(otherSplit.isSettled(), "Other member's share must be unsettled");
    }

    @Test
    @DisplayName("listExpenses computes positive myBalance for payer and negative myBalance for debtor")
    void listExpenses_computesPerExpenseBalanceForPayerAndDebtor() {
        GroupExpense exp = GroupExpense.builder()
                .id(77L).groupId(5L).paidBy(10L).amount(new BigDecimal("100.00")).description("Snacks").build();
        when(expenseRepo.findByGroupIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(exp));

        GroupExpenseSplit s1 = GroupExpenseSplit.builder()
                .expenseId(77L).memberId(10L).amountOwed(new BigDecimal("50.00")).isSettled(true).build();
        GroupExpenseSplit s2 = GroupExpenseSplit.builder()
                .expenseId(77L).memberId(20L).amountOwed(new BigDecimal("50.00")).isSettled(false).build();
        when(splitRepo.findByExpenseId(77L)).thenReturn(List.of(s1, s2));

        List<GroupExpense> forPayer = service.listExpenses(5L, 10L);
        assertEquals(new BigDecimal("50.00"), forPayer.get(0).getMyBalance());

        List<GroupExpense> forDebtor = service.listExpenses(5L, 20L);
        assertEquals(new BigDecimal("-50.00"), forDebtor.get(0).getMyBalance());
    }
}
