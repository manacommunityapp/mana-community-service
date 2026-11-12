package com.manacommunity.api.socialcircle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.socialcircle.dto.KittyPartyHousieDtos.*;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kitty Party Housie Engine Tests")
class KittyPartyHousieEngineTest {

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
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private KittyPartyHousieEngine housieEngine;

    private AppUser testUser;
    private KittyPartyCircle testCircle;
    private KittyPartyGameRoom testRoom;
    private KittyPartyGamePlayer testPlayer;

    @BeforeEach
    void setUp() {
        testUser = new AppUser();
        testUser.setId(101L);
        testUser.setEmail("kitty_host@example.com");
        testUser.setFullName("Host User");

        testCircle = KittyPartyCircle.builder()
                .id(1L)
                .circleName("Weekend Kitties")
                .createdBy(testUser)
                .build();

        testRoom = KittyPartyGameRoom.builder()
                .id(50L)
                .circle(testCircle)
                .roomName("Tambola Fun")
                .gameType("TAMBOLA_90")
                .status("IN_PROGRESS")
                .maxPlayers(50)
                .createdBy(testUser)
                .build();

        testPlayer = KittyPartyGamePlayer.builder()
                .id(77L)
                .room(testRoom)
                .user(testUser)
                .build();
    }

    @Test
    @DisplayName("GIVEN 90-ball Tambola generator WHEN generating matrix THEN verify 3x9 grid with exactly 15 numbers and 5 per row")
    @SuppressWarnings("unchecked")
    void testTambolaMatrixStructure() throws Exception {
        Method generateMethod = KittyPartyHousieEngine.class.getDeclaredMethod("generate90BallTambolaMatrix");
        generateMethod.setAccessible(true);

        List<List<Integer>> matrix = (List<List<Integer>>) generateMethod.invoke(housieEngine);

        assertNotNull(matrix);
        assertEquals(3, matrix.size(), "Ticket must have 3 rows");

        int totalNumbers = 0;
        int[][] colRanges = {
                {1, 9}, {10, 19}, {20, 29}, {30, 39}, {40, 49},
                {50, 59}, {60, 69}, {70, 79}, {80, 90}
        };

        for (int r = 0; r < 3; r++) {
            List<Integer> row = matrix.get(r);
            assertEquals(9, row.size(), "Each row must have 9 columns");
            long numbersInRow = row.stream().filter(n -> n > 0).count();
            assertEquals(5, numbersInRow, "Each row must have exactly 5 numbers");
            totalNumbers += numbersInRow;
        }

        assertEquals(15, totalNumbers, "Ticket must contain exactly 15 numbers");

        // Verify column ranges and ascending order per column
        for (int c = 0; c < 9; c++) {
            List<Integer> colValues = new ArrayList<>();
            for (int r = 0; r < 3; r++) {
                int val = matrix.get(r).get(c);
                if (val > 0) {
                    assertTrue(val >= colRanges[c][0] && val <= colRanges[c][1],
                            "Value " + val + " not in column range [" + colRanges[c][0] + ", " + colRanges[c][1] + "]");
                    colValues.add(val);
                }
            }
            assertTrue(colValues.size() >= 1 && colValues.size() <= 3, "Column must have between 1 and 3 numbers");
            for (int i = 0; i < colValues.size() - 1; i++) {
                assertTrue(colValues.get(i) < colValues.get(i + 1), "Column numbers must be in ascending order");
            }
        }
    }

    @Test
    @DisplayName("GIVEN active room WHEN next number called THEN draw from remaining 1-90 pool")
    void testCallNextNumber() {
        when(roomRepository.findById(50L)).thenReturn(Optional.of(testRoom));
        when(numberCallRepository.findByRoomIdOrderBySequenceNumAsc(50L)).thenReturn(Collections.emptyList());
        when(numberCallRepository.save(any(KittyPartyGameNumberCall.class))).thenAnswer(i -> i.getArgument(0));

        CallNumberResponse response = housieEngine.callNextNumber(50L, testUser);

        assertNotNull(response);
        assertEquals(50L, response.getRoomId());
        assertEquals(1, response.getSequenceNum());
        assertTrue(response.getCalledNumber() >= 1 && response.getCalledNumber() <= 90);
        verify(numberCallRepository, times(1)).save(any(KittyPartyGameNumberCall.class));
    }

    @Test
    @DisplayName("GIVEN ticket matrix WHEN validating claims THEN verify Early 5, Top Line, Full House logic")
    @SuppressWarnings("unchecked")
    void testValidateClaims() throws Exception {
        Method validateMethod = KittyPartyHousieEngine.class.getDeclaredMethod("validateClaim", String.class, List.class, Set.class);
        validateMethod.setAccessible(true);

        List<List<Integer>> matrix = List.of(
                List.of(4, 0, 22, 0, 41, 0, 63, 0, 85),
                List.of(0, 12, 0, 33, 0, 52, 0, 74, 88),
                List.of(7, 0, 29, 38, 0, 58, 69, 0, 0)
        );

        // Top line numbers: 4, 22, 41, 63, 85
        // Not enough called
        Set<Integer> called1 = Set.of(4, 22, 41);
        boolean earlyFiveFail = (boolean) validateMethod.invoke(housieEngine, "EARLY_FIVE", matrix, called1);
        assertFalse(earlyFiveFail);

        // 5 numbers called
        Set<Integer> called2 = Set.of(4, 22, 41, 12, 7);
        boolean earlyFivePass = (boolean) validateMethod.invoke(housieEngine, "EARLY_FIVE", matrix, called2);
        assertTrue(earlyFivePass);

        // Top line
        Set<Integer> topLineNumbers = Set.of(4, 22, 41, 63, 85);
        boolean topLinePass = (boolean) validateMethod.invoke(housieEngine, "TOP_LINE", matrix, topLineNumbers);
        assertTrue(topLinePass);

        // Four corners: top-left (4), top-right (85), bot-left (7), bot-right (69)
        Set<Integer> cornersNumbers = Set.of(4, 85, 7, 69);
        boolean fourCornersPass = (boolean) validateMethod.invoke(housieEngine, "FOUR_CORNERS", matrix, cornersNumbers);
        assertTrue(fourCornersPass);

        // Full house with only partial
        boolean fullHouseFail = (boolean) validateMethod.invoke(housieEngine, "FULL_HOUSE", matrix, cornersNumbers);
        assertFalse(fullHouseFail);

        // Full house with all 15 numbers
        Set<Integer> all15 = Set.of(4, 22, 41, 63, 85, 12, 33, 52, 74, 88, 7, 29, 38, 58, 69);
        boolean fullHousePass = (boolean) validateMethod.invoke(housieEngine, "FULL_HOUSE", matrix, all15);
        assertTrue(fullHousePass);
    }
}
