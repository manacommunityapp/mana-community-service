package com.manacommunity.api.socialcircle.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.resident.repository.FlatMembershipRepository;
import com.manacommunity.api.socialcircle.dto.KittyPartyLeaderboardDtos.*;
import com.manacommunity.api.socialcircle.dto.KittyPartyPostDtos.*;
import com.manacommunity.api.socialcircle.dto.KittyPartyVendorPackageDtos.*;
import com.manacommunity.api.socialcircle.entity.*;
import com.manacommunity.api.socialcircle.repository.*;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kitty Party Phase 5 Features Test Suite (Leaderboard, Polls, Vendors)")
class KittyPartyPhase5FeaturesTest {

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
    private KittyPartyCircleRepository circleRepository;
    @Mock
    private KittyPartyMemberRepository memberRepository;
    @Mock
    private FlatMembershipRepository flatMembershipRepository;

    @InjectMocks
    private KittyPartyHousieEngine housieEngine;

    @Mock
    private KittyPartyPostRepository postRepository;
    @Mock
    private KittyPartyPostCommentRepository commentRepository;
    @Mock
    private KittyPartyPollOptionRepository pollOptionRepository;
    @Mock
    private KittyPartyPollVoteRepository pollVoteRepository;

    @InjectMocks
    private KittyPartyPostService postService;

    @Mock
    private KittyPartyVendorPackageRepository vendorPackageRepository;
    @Mock
    private CommunityRepository communityRepository;

    @InjectMocks
    private KittyPartyVendorPackageService vendorPackageService;

    private Community testCommunity;
    private KittyPartyCircle testCircle;
    private AppUser player1;
    private AppUser player2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(housieEngine, "flatMembershipRepository", flatMembershipRepository);

        testCommunity = new Community();
        testCommunity.setId(1L);
        testCommunity.setName("Palm Meadows Society");

        player1 = new AppUser();
        player1.setId(10L);
        player1.setFullName("Priya Sharma");
        player1.setCommunity(testCommunity);

        player2 = new AppUser();
        player2.setId(20L);
        player2.setFullName("Ananya Verma");
        player2.setCommunity(testCommunity);

        testCircle = KittyPartyCircle.builder()
                .id(100L)
                .community(testCommunity)
                .circleName("Golden Kitties Club")
                .createdBy(player1)
                .build();
    }

    @Test
    @DisplayName("Leaderboard: Aggregates games played, claims won, points and assigns rank")
    void testGetCircleLeaderboard() {
        when(circleRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(testCircle));

        KittyPartyGameRoom room1 = KittyPartyGameRoom.builder().id(501L).circle(testCircle).build();
        KittyPartyGameRoom room2 = KittyPartyGameRoom.builder().id(502L).circle(testCircle).build();
        when(roomRepository.findByCircleIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(room1, room2));

        KittyPartyGamePlayer p1r1 = KittyPartyGamePlayer.builder().id(1L).room(room1).user(player1).build();
        KittyPartyGamePlayer p2r1 = KittyPartyGamePlayer.builder().id(2L).room(room1).user(player2).build();
        KittyPartyGamePlayer p1r2 = KittyPartyGamePlayer.builder().id(3L).room(room2).user(player1).build();

        when(playerRepository.findByRoomIdIn(List.of(501L, 502L))).thenReturn(List.of(p1r1, p2r1, p1r2));

        KittyPartyGameWinner w1 = KittyPartyGameWinner.builder()
                .id(1L)
                .room(room1)
                .user(player1)
                .claimType("FULL_HOUSE")
                .claimStatus("VALIDATED")
                .build();

        KittyPartyGameWinner w2 = KittyPartyGameWinner.builder()
                .id(2L)
                .room(room1)
                .user(player2)
                .claimType("EARLY_FIVE")
                .claimStatus("VALIDATED")
                .build();

        when(winnerRepository.findByRoomIdIn(List.of(501L, 502L))).thenReturn(List.of(w1, w2));

        CircleLeaderboardResponse response = housieEngine.getCircleLeaderboard(100L);

        assertNotNull(response);
        assertEquals(100L, response.getCircleId());
        assertEquals("Golden Kitties Club", response.getCircleName());
        assertEquals(2, response.getTotalGamesInCircle());
        assertEquals(2, response.getEntries().size());

        LeaderboardEntry rank1 = response.getEntries().get(0);
        assertEquals(10L, rank1.getUserId());
        assertEquals("Priya Sharma", rank1.getUserName());
        assertEquals(1, rank1.getRank());
        assertEquals(2, rank1.getTotalGamesPlayed()); // played 2 rooms
        assertEquals(1, rank1.getTotalWins());
        // Points: 2 games * 5 pts + 1 Full House * 50 pts = 60 pts
        assertEquals(60, rank1.getTotalPoints());
        assertTrue(rank1.getBadges().contains("👑 Full House Champion"));

        LeaderboardEntry rank2 = response.getEntries().get(1);
        assertEquals(20L, rank2.getUserId());
        assertEquals(2, rank2.getRank());
        assertEquals(1, rank2.getTotalGamesPlayed());
        // Points: 1 game * 5 pts + 1 Early Five * 20 pts = 25 pts
        assertEquals(25, rank2.getTotalPoints());
        assertTrue(rank2.getBadges().contains("⚡ Early Bird"));
    }

    @Test
    @DisplayName("Polls: Creates poll with options and enforces single-vote per resident")
    void testPollCreationAndVoting() {
        when(circleRepository.findById(100L)).thenReturn(Optional.of(testCircle));

        CreatePostRequest postReq = CreatePostRequest.builder()
                .postType("POLL")
                .title("Next Kitty Party Theme")
                .content("Vote for next Saturday's party theme!")
                .pollOptions(List.of("Bollywood Retro", "Hawaiian Luau", "Royal Silk & Saree"))
                .build();

        KittyPartyPost mockSavedPost = KittyPartyPost.builder()
                .id(300L)
                .circle(testCircle)
                .author(player1)
                .postType("POLL")
                .title("Next Kitty Party Theme")
                .content("Vote for next Saturday's party theme!")
                .isPinned(false)
                .status("PUBLISHED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(postRepository.save(any(KittyPartyPost.class))).thenReturn(mockSavedPost);

        PostResponse postRes = postService.createPost(player1, 100L, postReq);
        assertNotNull(postRes);
        assertEquals(300L, postRes.getId());
        assertEquals("POLL", postRes.getPostType());
        verify(pollOptionRepository, times(3)).save(any(KittyPartyPollOption.class));

        // Test Voting
        KittyPartyPollOption opt1 = KittyPartyPollOption.builder().id(11L).post(mockSavedPost).optionText("Bollywood Retro").build();
        when(postRepository.findById(300L)).thenReturn(Optional.of(mockSavedPost));
        when(pollOptionRepository.findById(11L)).thenReturn(Optional.of(opt1));
        when(pollVoteRepository.existsByPostIdAndUserId(300L, player2.getId())).thenReturn(false);

        when(pollVoteRepository.countByPostId(300L)).thenReturn(1L);
        when(pollVoteRepository.findByPostIdAndUserId(300L, player2.getId()))
                .thenReturn(Optional.of(KittyPartyPollVote.builder().id(99L).post(mockSavedPost).option(opt1).user(player2).build()));
        when(pollOptionRepository.findByPostIdOrderBySortOrderAsc(300L)).thenReturn(List.of(opt1));
        when(pollVoteRepository.countByOptionId(11L)).thenReturn(1L);

        PostResponse voteRes = postService.castVote(player2, 300L, 11L);
        assertNotNull(voteRes);
        assertEquals(1L, voteRes.getTotalVotes());
        assertEquals(11L, voteRes.getUserVotedOptionId());
        assertEquals(100.0, voteRes.getPollOptions().get(0).getPercentage());
        assertTrue(voteRes.getPollOptions().get(0).isUserVoted());

        // Verify duplicate voting throws IllegalStateException
        when(pollVoteRepository.existsByPostIdAndUserId(300L, player2.getId())).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> postService.castVote(player2, 300L, 11L));
    }

    @Test
    @DisplayName("Vendor Packages: Discovers curated packages and seeds defaults if empty")
    void testVendorPackagesDiscoveryAndSeeding() {
        when(circleRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(testCircle));
        when(vendorPackageRepository.existsByCommunityId(1L)).thenReturn(false);

        KittyPartyVendorPackage cateringPkg = KittyPartyVendorPackage.builder()
                .id(701L)
                .community(testCommunity)
                .category("CATERING")
                .packageName("Royal Chaat Spread")
                .priceRange("₹450 / guest")
                .contactPerson("Chef Rajesh")
                .rating(BigDecimal.valueOf(4.90))
                .isActive(true)
                .build();

        when(vendorPackageRepository.findByCommunityIdAndIsActiveTrueOrderByRatingDesc(1L))
                .thenReturn(List.of(cateringPkg));

        List<VendorPackageResponse> packages = vendorPackageService.getPackagesForCircle(100L, null);

        assertNotNull(packages);
        assertEquals(1, packages.size());
        assertEquals("Royal Chaat Spread", packages.get(0).getPackageName());
        assertEquals("CATERING", packages.get(0).getCategory());
        verify(vendorPackageRepository).saveAll(anyList()); // default packages seeded
    }
}
