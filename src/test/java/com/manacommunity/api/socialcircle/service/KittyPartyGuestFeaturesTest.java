package com.manacommunity.api.socialcircle.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.socialcircle.dto.KittyPartyCircleDtos.*;
import com.manacommunity.api.socialcircle.dto.KittyPartyEventDtos.*;
import com.manacommunity.api.socialcircle.dto.KittyPartyHousieDtos.*;
import com.manacommunity.api.socialcircle.entity.*;
import com.manacommunity.api.socialcircle.repository.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kitty Party Guest Features Test Suite (External Circle Members, Gate Passes, Housie Room Codes)")
class KittyPartyGuestFeaturesTest {

    @Mock
    private KittyPartyCircleRepository circleRepository;
    @Mock
    private KittyPartyMemberRepository memberRepository;
    @Mock
    private KittyPartyInvitationRepository invitationRepository;
    @Mock
    private KittyPartyCycleRepository cycleRepository;
    @Mock
    private CommunityRepository communityRepository;
    @Mock
    private AppUserRepository userRepository;

    @InjectMocks
    private KittyPartyCircleService circleService;

    @Mock
    private KittyPartyEventRepository eventRepository;
    @Mock
    private KittyPartyEventRsvpRepository rsvpRepository;
    @Mock
    private KittyPartyEventAttendanceRepository attendanceRepository;

    @InjectMocks
    private KittyPartyEventService eventService;

    @Mock
    private KittyPartyGameRoomRepository roomRepository;
    @Mock
    private KittyPartyGamePlayerRepository playerRepository;
    @Mock
    private KittyPartyGameTicketRepository ticketRepository;
    @Mock
    private KittyPartyGameNumberCallRepository numberCallRepository;
    @Mock
    private KittyPartyGameWinnerRepository winnerRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private KittyPartyHousieEngine housieEngine;

    private AppUser residentUser;
    private AppUser externalGuestUser;
    private Community community;
    private KittyPartyCircle openCircle;
    private KittyPartyCircle closedCircle;
    private KittyPartyEvent event;

    @BeforeEach
    void setUp() {
        community = new Community();
        community.setId(10L);
        community.setName("Green Valley");

        residentUser = new AppUser();
        residentUser.setId(1L);
        residentUser.setFullName("Pooja Sharma");
        residentUser.setPhone("9876543210");
        residentUser.setCommunity(community);

        externalGuestUser = new AppUser();
        externalGuestUser.setId(2L);
        externalGuestUser.setFullName("Rohit Verma (Guest)");
        externalGuestUser.setPhone("9123456789");
        externalGuestUser.setResidentType("GUEST");

        openCircle = KittyPartyCircle.builder()
                .id(100L)
                .community(community)
                .circleName("Rose Circle")
                .status("ACTIVE")
                .allowExternalMembers(true)
                .maxExternalMembers(3)
                .createdBy(residentUser)
                .build();

        closedCircle = KittyPartyCircle.builder()
                .id(200L)
                .community(community)
                .circleName("Exclusive Society Circle")
                .status("ACTIVE")
                .allowExternalMembers(false)
                .maxExternalMembers(0)
                .createdBy(residentUser)
                .build();

        event = KittyPartyEvent.builder()
                .id(300L)
                .circle(openCircle)
                .eventName("Diwali Milan Kitty")
                .venueType("HOST_HOME")
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(3))
                .status("SCHEDULED")
                .createdBy(residentUser)
                .build();
    }

    // =========================================================================
    // 1. External Circle Members Tests
    // =========================================================================

    @Test
    @DisplayName("Should successfully invite an external guest when circle allows external members")
    void testInviteExternalGuestSuccess() {
        when(circleRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(openCircle));
        when(memberRepository.findByCircleIdAndUserId(100L, residentUser.getId())).thenReturn(Optional.of(
                KittyPartyMember.builder().circle(openCircle).user(residentUser).roleCode("ADMIN").build()
        ));
        when(memberRepository.countByCircleIdAndIsExternalTrueAndMembershipStatus(100L, "ACTIVE")).thenReturn(1L);
        when(userRepository.findByPhone("9123456789")).thenReturn(Optional.of(externalGuestUser));
        when(memberRepository.findByCircleIdAndUserId(100L, externalGuestUser.getId())).thenReturn(Optional.empty());

        when(memberRepository.save(any(KittyPartyMember.class))).thenAnswer(invocation -> {
            KittyPartyMember m = invocation.getArgument(0);
            m.setId(555L);
            return m;
        });
        when(invitationRepository.save(any(KittyPartyInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InviteRequest request = InviteRequest.builder()
                .invitedPhone("9123456789")
                .isExternal(true)
                .externalSocietyName("Palm Meadows")
                .build();

        String token = circleService.inviteMember(100L, residentUser, request);

        assertNotNull(token);
        verify(memberRepository).save(argThat(m ->
                Boolean.TRUE.equals(m.getIsExternal()) &&
                "Palm Meadows".equals(m.getExternalSocietyName()) &&
                residentUser.equals(m.getSponsorUser())
        ));
    }

    @Test
    @DisplayName("Should reject external guest when circle forbids external members")
    void testInviteExternalGuestForbidden() {
        when(circleRepository.findByIdAndDeletedAtIsNull(200L)).thenReturn(Optional.of(closedCircle));
        when(memberRepository.findByCircleIdAndUserId(200L, residentUser.getId())).thenReturn(Optional.of(
                KittyPartyMember.builder().circle(closedCircle).user(residentUser).roleCode("OWNER").build()
        ));

        InviteRequest request = InviteRequest.builder()
                .invitedPhone("9123456789")
                .isExternal(true)
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                circleService.inviteMember(200L, residentUser, request));
        assertTrue(ex.getMessage().contains("does not allow external"));
    }

    @Test
    @DisplayName("Should reject external guest when maxExternalMembers cap is reached")
    void testInviteExternalGuestCapReached() {
        when(circleRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(openCircle));
        when(memberRepository.findByCircleIdAndUserId(100L, residentUser.getId())).thenReturn(Optional.of(
                KittyPartyMember.builder().circle(openCircle).user(residentUser).roleCode("ADMIN").build()
        ));
        // Already at max of 3
        when(memberRepository.countByCircleIdAndIsExternalTrueAndMembershipStatus(100L, "ACTIVE")).thenReturn(3L);

        InviteRequest request = InviteRequest.builder()
                .invitedPhone("9123456789")
                .isExternal(true)
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                circleService.inviteMember(100L, residentUser, request));
        assertTrue(ex.getMessage().contains("Maximum external guest"));
    }

    // =========================================================================
    // 2. Automated Gate Pass on Event RSVP Tests
    // =========================================================================

    @Test
    @DisplayName("Should generate gate pass code when RSVP response is GOING")
    void testGatePassGeneratedOnRsvpGoing() {
        when(eventRepository.findById(300L)).thenReturn(Optional.of(event));
        when(rsvpRepository.findByEventIdAndUserId(300L, externalGuestUser.getId())).thenReturn(Optional.empty());
        when(rsvpRepository.save(any(KittyPartyEventRsvp.class))).thenAnswer(invocation -> {
            KittyPartyEventRsvp rsvp = invocation.getArgument(0);
            rsvp.setId(999L);
            return rsvp;
        });

        RsvpRequest request = RsvpRequest.builder()
                .response("GOING")
                .guestCount(1)
                .notes("Coming with spouse")
                .build();

        RsvpResponse response = eventService.submitRsvp(externalGuestUser, 300L, request);

        assertNotNull(response);
        assertEquals("GOING", response.getResponse());
        assertNotNull(response.getGatePassCode());
        assertTrue(response.getGatePassCode().startsWith("GP-"));
        assertNotNull(response.getGatePassGeneratedAt());
    }

    @Test
    @DisplayName("Should clear gate pass code if RSVP is changed to NOT_GOING")
    void testGatePassClearedOnRsvpNotGoing() {
        KittyPartyEventRsvp existingRsvp = KittyPartyEventRsvp.builder()
                .id(999L)
                .event(event)
                .user(externalGuestUser)
                .response("GOING")
                .gatePassCode("GP-ABC123")
                .gatePassGeneratedAt(LocalDateTime.now().minusHours(1))
                .build();

        when(eventRepository.findById(300L)).thenReturn(Optional.of(event));
        when(rsvpRepository.findByEventIdAndUserId(300L, externalGuestUser.getId())).thenReturn(Optional.of(existingRsvp));
        when(rsvpRepository.save(any(KittyPartyEventRsvp.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RsvpRequest request = RsvpRequest.builder()
                .response("NOT_GOING")
                .build();

        RsvpResponse response = eventService.submitRsvp(externalGuestUser, 300L, request);

        assertNotNull(response);
        assertEquals("NOT_GOING", response.getResponse());
        assertNull(response.getGatePassCode());
        assertNull(response.getGatePassGeneratedAt());
    }

    // =========================================================================
    // 3. Housie Room Code & Join-By-Code Tests
    // =========================================================================

    @Test
    @DisplayName("Should create Housie room with 6-character roomCode and allowGuestPlayers flag")
    void testCreateRoomWithCode() {
        when(circleRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(openCircle));
        when(roomRepository.findByRoomCode(anyString())).thenReturn(Optional.empty());
        when(roomRepository.save(any(KittyPartyGameRoom.class))).thenAnswer(invocation -> {
            KittyPartyGameRoom r = invocation.getArgument(0);
            r.setId(400L);
            return r;
        });
        when(roomRepository.findById(400L)).thenAnswer(inv -> Optional.of(KittyPartyGameRoom.builder()
                .id(400L)
                .circle(openCircle)
                .roomName("Friday Housie")
                .roomCode("KP1234")
                .allowGuestPlayers(true)
                .gameType("TAMBOLA_90")
                .status("WAITING")
                .maxPlayers(50)
                .createdBy(residentUser)
                .build()));
        when(playerRepository.existsByRoomIdAndUserId(400L, residentUser.getId())).thenReturn(false);
        when(playerRepository.countByRoomId(400L)).thenReturn(1L);
        when(numberCallRepository.findByRoomIdOrderBySequenceNumAsc(400L)).thenReturn(Collections.emptyList());
        when(winnerRepository.findByRoomIdOrderByClaimedAtAsc(400L)).thenReturn(Collections.emptyList());

        CreateRoomRequest request = CreateRoomRequest.builder()
                .roomName("Friday Housie")
                .allowGuestPlayers(true)
                .build();

        GameRoomResponse response = housieEngine.createRoom(100L, residentUser, request);

        assertNotNull(response);
        assertEquals(400L, response.getId());
        assertNotNull(response.getRoomCode());
        assertTrue(response.getAllowGuestPlayers());
    }

    @Test
    @DisplayName("Should successfully join Housie room by room code")
    void testJoinRoomByCodeSuccess() {
        KittyPartyGameRoom room = KittyPartyGameRoom.builder()
                .id(400L)
                .circle(openCircle)
                .roomName("Friday Housie")
                .roomCode("PLAY99")
                .allowGuestPlayers(true)
                .gameType("TAMBOLA_90")
                .status("WAITING")
                .maxPlayers(50)
                .createdBy(residentUser)
                .build();

        when(roomRepository.findByRoomCode("PLAY99")).thenReturn(Optional.of(room));
        when(roomRepository.findById(400L)).thenReturn(Optional.of(room));
        when(playerRepository.existsByRoomIdAndUserId(400L, externalGuestUser.getId())).thenReturn(false);
        when(playerRepository.countByRoomId(400L)).thenReturn(2L);
        when(numberCallRepository.findByRoomIdOrderBySequenceNumAsc(400L)).thenReturn(Collections.emptyList());
        when(winnerRepository.findByRoomIdOrderByClaimedAtAsc(400L)).thenReturn(Collections.emptyList());

        GameRoomResponse response = housieEngine.joinRoomByCode(externalGuestUser, "PLAY99");

        assertNotNull(response);
        assertEquals(400L, response.getId());
        assertEquals("PLAY99", response.getRoomCode());
        verify(playerRepository, times(1)).save(any(KittyPartyGamePlayer.class));
    }

    @Test
    @DisplayName("Should deny joining room by code if guest players not allowed and user is not circle member")
    void testJoinRoomByCodeGuestsForbidden() {
        KittyPartyGameRoom room = KittyPartyGameRoom.builder()
                .id(400L)
                .circle(openCircle)
                .roomName("Private Members Housie")
                .roomCode("PRIV88")
                .allowGuestPlayers(false)
                .gameType("TAMBOLA_90")
                .status("WAITING")
                .createdBy(residentUser)
                .build();

        when(roomRepository.findByRoomCode("PRIV88")).thenReturn(Optional.of(room));
        when(memberRepository.existsByCircleIdAndUserIdAndMembershipStatus(openCircle.getId(), externalGuestUser.getId(), "ACTIVE"))
                .thenReturn(false);

        SecurityException ex = assertThrows(SecurityException.class, () ->
                housieEngine.joinRoomByCode(externalGuestUser, "PRIV88"));
        assertTrue(ex.getMessage().contains("Guest players are not allowed in this game room"));
    }
}
