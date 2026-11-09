package com.manacommunity.api.socialcircle.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.resident.repository.FlatMembershipRepository;
import com.manacommunity.api.socialcircle.dto.KittyPartyHousieDtos.*;
import com.manacommunity.api.socialcircle.dto.KittyPartyLeaderboardDtos.*;
import com.manacommunity.api.socialcircle.entity.*;
import com.manacommunity.api.socialcircle.repository.*;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KittyPartyHousieEngine {

    private final KittyPartyGameRoomRepository roomRepository;
    private final KittyPartyGamePlayerRepository playerRepository;
    private final KittyPartyGameTicketRepository ticketRepository;
    private final KittyPartyGameNumberCallRepository numberCallRepository;
    private final KittyPartyGameWinnerRepository winnerRepository;
    private final KittyPartyCircleRepository circleRepository;
    private final KittyPartyMemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private FlatMembershipRepository flatMembershipRepository;

    @Transactional
    public GameRoomResponse createRoom(Long circleId, AppUser creator, CreateRoomRequest request) {
        KittyPartyCircle circle = circleRepository.findByIdAndDeletedAtIsNull(circleId)
                .orElseThrow(() -> new IllegalArgumentException("Circle not found"));

        String roomCode = generateUniqueRoomCode();
        boolean allowGuests = request.getAllowGuestPlayers() != null ? request.getAllowGuestPlayers() : true;

        KittyPartyGameRoom room = KittyPartyGameRoom.builder()
                .circle(circle)
                .roomName(request.getRoomName().trim())
                .roomCode(roomCode)
                .allowGuestPlayers(allowGuests)
                .gameType("TAMBOLA_90")
                .status("WAITING")
                .maxPlayers(request.getMaxPlayers() != null ? request.getMaxPlayers() : 50)
                .createdBy(creator)
                .build();

        KittyPartyGameRoom saved = roomRepository.save(room);
        joinRoom(saved.getId(), creator);
        return getRoomDetails(saved.getId());
    }

    @Transactional
    public void joinRoom(Long roomId, AppUser user) {
        KittyPartyGameRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Game room not found"));

        if (!"WAITING".equals(room.getStatus()) && !"IN_PROGRESS".equals(room.getStatus())) {
            throw new IllegalStateException("Room is not accepting new players");
        }

        if (playerRepository.existsByRoomIdAndUserId(roomId, user.getId())) {
            return;
        }

        KittyPartyGamePlayer player = KittyPartyGamePlayer.builder()
                .room(room)
                .user(user)
                .build();
        playerRepository.save(player);
    }

    @Transactional
    public GameRoomResponse joinRoomByCode(AppUser user, String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            throw new IllegalArgumentException("Room code is required");
        }
        String cleanCode = roomCode.trim().toUpperCase();
        KittyPartyGameRoom room = roomRepository.findByRoomCode(cleanCode)
                .orElseThrow(() -> new IllegalArgumentException("No game room found with code: " + cleanCode));

        if (!Boolean.TRUE.equals(room.getAllowGuestPlayers())) {
            boolean isMember = memberRepository.existsByCircleIdAndUserIdAndMembershipStatus(
                    room.getCircle().getId(), user.getId(), "ACTIVE");
            if (!isMember) {
                throw new SecurityException("Guest players are not allowed in this game room. You must be an active circle member.");
            }
        }

        joinRoom(room.getId(), user);
        return getRoomDetails(room.getId());
    }

    @Transactional
    public TicketResponse generateTicketForPlayer(Long roomId, AppUser user) {
        KittyPartyGameRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        KittyPartyGamePlayer player = playerRepository.findByRoomIdAndUserId(roomId, user.getId())
                .orElseGet(() -> {
                    KittyPartyGamePlayer p = KittyPartyGamePlayer.builder().room(room).user(user).build();
                    return playerRepository.save(p);
                });

        List<List<Integer>> matrix = generate90BallTambolaMatrix();
        String json;
        try {
            json = objectMapper.writeValueAsString(matrix);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize ticket matrix", e);
        }

        String hash = computeSha256(roomId + ":" + player.getId() + ":" + json + ":" + System.nanoTime());

        KittyPartyGameTicket ticket = KittyPartyGameTicket.builder()
                .room(room)
                .player(player)
                .ticketMatrix(json)
                .ticketHash(hash)
                .build();

        KittyPartyGameTicket saved = ticketRepository.save(ticket);

        return TicketResponse.builder()
                .ticketId(saved.getId())
                .roomId(roomId)
                .playerId(player.getId())
                .matrix(matrix)
                .ticketHash(hash)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getPlayerTickets(Long roomId, Long userId) {
        KittyPartyGamePlayer player = playerRepository.findByRoomIdAndUserId(roomId, userId).orElse(null);
        if (player == null) return Collections.emptyList();

        List<KittyPartyGameTicket> tickets = ticketRepository.findByRoomIdAndPlayerId(roomId, player.getId());
        return tickets.stream().map(t -> {
            try {
                List<List<Integer>> matrix = objectMapper.readValue(t.getTicketMatrix(), new TypeReference<>() {});
                return TicketResponse.builder()
                        .ticketId(t.getId())
                        .roomId(roomId)
                        .playerId(player.getId())
                        .matrix(matrix)
                        .ticketHash(t.getTicketHash())
                        .build();
            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Transactional
    public void startGame(Long roomId, AppUser caller) {
        KittyPartyGameRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (!room.getCreatedBy().getId().equals(caller.getId())) {
            assertAdminOrOwner(room.getCircle().getId(), caller.getId());
        }

        room.setStatus("IN_PROGRESS");
        room.setStartedAt(LocalDateTime.now());
        roomRepository.save(room);

        broadcast(roomId, Map.of("type", "GAME_STARTED", "roomId", roomId));
    }

    @Transactional
    public CallNumberResponse callNextNumber(Long roomId, AppUser caller) {
        KittyPartyGameRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (!"IN_PROGRESS".equals(room.getStatus())) {
            throw new IllegalStateException("Game is not in progress");
        }

        // Get all already called numbers
        List<KittyPartyGameNumberCall> calledList = numberCallRepository.findByRoomIdOrderBySequenceNumAsc(roomId);
        Set<Integer> calledSet = calledList.stream()
                .map(KittyPartyGameNumberCall::getCalledNumber)
                .collect(Collectors.toSet());

        if (calledSet.size() >= 90) {
            room.setStatus("COMPLETED");
            room.setCompletedAt(LocalDateTime.now());
            roomRepository.save(room);
            throw new IllegalStateException("All 90 numbers have been called");
        }

        List<Integer> pool = new ArrayList<>();
        for (int i = 1; i <= 90; i++) {
            if (!calledSet.contains(i)) {
                pool.add(i);
            }
        }

        Collections.shuffle(pool, new Random());
        int nextNumber = pool.get(0);
        int seq = calledList.size() + 1;

        KittyPartyGameNumberCall numberCall = KittyPartyGameNumberCall.builder()
                .room(room)
                .sequenceNum(seq)
                .calledNumber(nextNumber)
                .build();
        numberCallRepository.save(numberCall);

        CallNumberResponse response = CallNumberResponse.builder()
                .roomId(roomId)
                .sequenceNum(seq)
                .calledNumber(nextNumber)
                .calledAt(LocalDateTime.now())
                .build();

        broadcast(roomId, Map.of("type", "NUMBER_CALLED", "payload", response));
        return response;
    }

    @Transactional
    public WinnerClaimResponse claimPrize(Long roomId, AppUser claimant, ClaimPrizeRequest request) {
        KittyPartyGameRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (!"IN_PROGRESS".equals(room.getStatus())) {
            throw new IllegalStateException("Game is not active");
        }

        String claimType = request.getClaimType().toUpperCase();

        // Check if prize already won
        if (winnerRepository.existsByRoomIdAndClaimType(roomId, claimType)) {
            throw new IllegalStateException("Prize '" + claimType + "' has already been claimed and awarded!");
        }

        KittyPartyGameTicket ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        if (!ticket.getPlayer().getUser().getId().equals(claimant.getId())) {
            throw new SecurityException("Ticket does not belong to the claiming user");
        }

        // Fetch called numbers
        Set<Integer> calledSet = numberCallRepository.findByRoomIdOrderBySequenceNumAsc(roomId)
                .stream().map(KittyPartyGameNumberCall::getCalledNumber).collect(Collectors.toSet());

        List<List<Integer>> matrix;
        try {
            matrix = objectMapper.readValue(ticket.getTicketMatrix(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Corrupted ticket matrix", e);
        }

        boolean isValid = validateClaim(claimType, matrix, calledSet);
        if (!isValid) {
            log.warn("Bogus claim by user {} for pattern {} in room {}", claimant.getId(), claimType, roomId);
            throw new IllegalArgumentException("Bogus claim! Numbers on your ticket for '" + claimType + "' do not match called numbers.");
        }

        KittyPartyGameWinner winner = KittyPartyGameWinner.builder()
                .room(room)
                .ticket(ticket)
                .user(claimant)
                .claimType(claimType)
                .claimStatus("VALIDATED")
                .build();

        KittyPartyGameWinner saved = winnerRepository.save(winner);

        WinnerClaimResponse response = WinnerClaimResponse.builder()
                .id(saved.getId())
                .roomId(roomId)
                .ticketId(ticket.getId())
                .userId(claimant.getId())
                .userName(claimant.getFullName())
                .claimType(claimType)
                .claimStatus("VALIDATED")
                .claimedAt(saved.getClaimedAt())
                .build();

        broadcast(roomId, Map.of("type", "WINNER_ANNOUNCED", "payload", response));

        // If Full House claimed, complete game
        if ("FULL_HOUSE".equals(claimType)) {
            room.setStatus("COMPLETED");
            room.setCompletedAt(LocalDateTime.now());
            roomRepository.save(room);
            broadcast(roomId, Map.of("type", "GAME_COMPLETED", "roomId", roomId));
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<GameRoomResponse> getCircleRooms(Long circleId) {
        circleRepository.findByIdAndDeletedAtIsNull(circleId)
                .orElseThrow(() -> new IllegalArgumentException("Circle not found"));

        List<KittyPartyGameRoom> rooms = roomRepository.findByCircleIdOrderByCreatedAtDesc(circleId);
        return rooms.stream().map(r -> getRoomDetails(r.getId())).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GameRoomResponse getRoomDetails(Long roomId) {
        KittyPartyGameRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        List<KittyPartyGameNumberCall> called = numberCallRepository.findByRoomIdOrderBySequenceNumAsc(roomId);
        List<Integer> numbers = called.stream().map(KittyPartyGameNumberCall::getCalledNumber).collect(Collectors.toList());
        Integer lastCalled = numbers.isEmpty() ? null : numbers.get(numbers.size() - 1);

        List<WinnerClaimResponse> winners = winnerRepository.findByRoomIdOrderByClaimedAtAsc(roomId).stream()
                .map(w -> WinnerClaimResponse.builder()
                        .id(w.getId())
                        .roomId(roomId)
                        .ticketId(w.getTicket().getId())
                        .userId(w.getUser().getId())
                        .userName(w.getUser().getFullName())
                        .claimType(w.getClaimType())
                        .claimStatus(w.getClaimStatus())
                        .claimedAt(w.getClaimedAt())
                        .build())
                .collect(Collectors.toList());

        long activePlayers = playerRepository.countByRoomId(roomId);

        return GameRoomResponse.builder()
                .id(room.getId())
                .circleId(room.getCircle().getId())
                .roomName(room.getRoomName())
                .roomCode(room.getRoomCode())
                .allowGuestPlayers(room.getAllowGuestPlayers())
                .gameType(room.getGameType())
                .status(room.getStatus())
                .maxPlayers(room.getMaxPlayers())
                .activePlayersCount(activePlayers)
                .createdById(room.getCreatedBy().getId())
                .createdByName(room.getCreatedBy().getFullName())
                .startedAt(room.getStartedAt())
                .completedAt(room.getCompletedAt())
                .lastCalledNumber(lastCalled)
                .calledNumbers(numbers)
                .winners(winners)
                .build();
    }

    private boolean validateClaim(String claimType, List<List<Integer>> matrix, Set<Integer> calledSet) {
        List<Integer> allNumbers = new ArrayList<>();
        List<Integer> topRow = new ArrayList<>();
        List<Integer> midRow = new ArrayList<>();
        List<Integer> botRow = new ArrayList<>();

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                int val = matrix.get(r).get(c);
                if (val > 0) {
                    allNumbers.add(val);
                    if (r == 0) topRow.add(val);
                    if (r == 1) midRow.add(val);
                    if (r == 2) botRow.add(val);
                }
            }
        }

        switch (claimType) {
            case "EARLY_FIVE":
                long matchCount = allNumbers.stream().filter(calledSet::contains).count();
                return matchCount >= 5;

            case "TOP_LINE":
                return !topRow.isEmpty() && calledSet.containsAll(topRow);

            case "MIDDLE_LINE":
                return !midRow.isEmpty() && calledSet.containsAll(midRow);

            case "BOTTOM_LINE":
                return !botRow.isEmpty() && calledSet.containsAll(botRow);

            case "FOUR_CORNERS":
                // Top-left, top-right, bottom-left, bottom-right numbers on ticket
                if (topRow.size() < 2 || botRow.size() < 2) return false;
                int topLeft = topRow.get(0);
                int topRight = topRow.get(topRow.size() - 1);
                int botLeft = botRow.get(0);
                int botRight = botRow.get(botRow.size() - 1);
                return calledSet.contains(topLeft) && calledSet.contains(topRight)
                        && calledSet.contains(botLeft) && calledSet.contains(botRight);

            case "FULL_HOUSE":
                return calledSet.containsAll(allNumbers);

            default:
                return false;
        }
    }

    /**
     * Generates standard Indian 90-ball Tambola matrix (3 rows x 9 columns, 15 numbers total, 5 per row).
     */
    /**
     * Generates standard Indian 90-ball Tambola matrix (3 rows x 9 columns, 15 numbers total, 5 per row).
     */
    private List<List<Integer>> generate90BallTambolaMatrix() {
        int[][] grid = new int[3][9];
        Random rand = new Random();

        // Column ranges: col 0: 1-9, col 1: 10-19, ..., col 8: 80-90
        int[][] colRanges = {
                {1, 9}, {10, 19}, {20, 29}, {30, 39}, {40, 49},
                {50, 59}, {60, 69}, {70, 79}, {80, 90}
        };

        // Shuffle column indices to randomly assign row positions
        List<Integer> cols = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8));
        Collections.shuffle(cols, rand);

        // Map which rows each column will occupy:
        // 6 columns have 2 numbers: two in rows {0, 1}, two in rows {0, 2}, two in rows {1, 2}
        // 3 columns have 1 number: one in row {0}, one in row {1}, one in row {2}
        // Result: Row 0 has 2+2+1=5 numbers, Row 1 has 2+2+1=5 numbers, Row 2 has 2+2+1=5 numbers.
        Map<Integer, List<Integer>> colToRows = new HashMap<>();
        colToRows.put(cols.get(0), List.of(0, 1));
        colToRows.put(cols.get(1), List.of(0, 1));
        colToRows.put(cols.get(2), List.of(0, 2));
        colToRows.put(cols.get(3), List.of(0, 2));
        colToRows.put(cols.get(4), List.of(1, 2));
        colToRows.put(cols.get(5), List.of(1, 2));
        colToRows.put(cols.get(6), List.of(0));
        colToRows.put(cols.get(7), List.of(1));
        colToRows.put(cols.get(8), List.of(2));

        for (int c = 0; c < 9; c++) {
            List<Integer> rowsForCol = colToRows.get(c);
            int count = rowsForCol.size();
            int min = colRanges[c][0];
            int max = colRanges[c][1];

            List<Integer> pool = new ArrayList<>();
            for (int v = min; v <= max; v++) pool.add(v);
            Collections.shuffle(pool, rand);
            List<Integer> picked = pool.subList(0, count);
            Collections.sort(picked);

            for (int i = 0; i < count; i++) {
                grid[rowsForCol.get(i)][c] = picked.get(i);
            }
        }

        List<List<Integer>> result = new ArrayList<>();
        for (int r = 0; r < 3; r++) {
            List<Integer> row = new ArrayList<>();
            for (int c = 0; c < 9; c++) {
                row.add(grid[r][c]);
            }
            result.add(row);
        }
        return result;
    }

    private void broadcast(Long roomId, Object payload) {
        try {
            messagingTemplate.convertAndSend("/topic/kitty-games/" + roomId, payload);
        } catch (Exception e) {
            log.warn("Failed to broadcast STOMP message for room {}", roomId, e);
        }
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : encoded) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private String generateUniqueRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder(6);
            for (int j = 0; j < 6; j++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            String code = sb.toString();
            if (roomRepository.findByRoomCode(code).isEmpty()) {
                return code;
            }
        }
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private void assertAdminOrOwner(Long circleId, Long userId) {
        var member = memberRepository.findByCircleIdAndUserId(circleId, userId)
                .orElseThrow(() -> new SecurityException("User is not a member of this circle"));
        if (!"OWNER".equals(member.getRoleCode()) && !"ADMIN".equals(member.getRoleCode())) {
            throw new SecurityException("Requires circle Admin or Owner role");
        }
    }

    @Transactional(readOnly = true)
    public CircleLeaderboardResponse getCircleLeaderboard(Long circleId) {
        KittyPartyCircle circle = circleRepository.findByIdAndDeletedAtIsNull(circleId)
                .orElseThrow(() -> new IllegalArgumentException("Circle not found"));

        List<KittyPartyGameRoom> rooms = roomRepository.findByCircleIdOrderByCreatedAtDesc(circleId);
        if (rooms.isEmpty()) {
            return CircleLeaderboardResponse.builder()
                    .circleId(circleId)
                    .circleName(circle.getCircleName())
                    .totalGamesInCircle(0)
                    .entries(Collections.emptyList())
                    .build();
        }

        List<Long> roomIds = rooms.stream().map(KittyPartyGameRoom::getId).collect(Collectors.toList());
        List<KittyPartyGamePlayer> allPlayers = playerRepository.findByRoomIdIn(roomIds);
        List<KittyPartyGameWinner> allWinners = winnerRepository.findByRoomIdIn(roomIds);

        Map<Long, AppUser> userMap = new HashMap<>();
        Map<Long, Set<Long>> userRoomsPlayed = new HashMap<>();

        for (KittyPartyGamePlayer p : allPlayers) {
            AppUser u = p.getUser();
            if (u != null) {
                userMap.put(u.getId(), u);
                userRoomsPlayed.computeIfAbsent(u.getId(), k -> new HashSet<>()).add(p.getRoom().getId());
            }
        }

        Map<Long, Map<String, Integer>> userWinBreakdown = new HashMap<>();
        Map<Long, Integer> userTotalWins = new HashMap<>();

        for (KittyPartyGameWinner w : allWinners) {
            if ("VALIDATED".equalsIgnoreCase(w.getClaimStatus())) {
                AppUser u = w.getUser();
                if (u != null) {
                    userMap.put(u.getId(), u);
                    userTotalWins.merge(u.getId(), 1, Integer::sum);
                    userWinBreakdown.computeIfAbsent(u.getId(), k -> new HashMap<>())
                            .merge(w.getClaimType(), 1, Integer::sum);
                }
            }
        }

        List<LeaderboardEntry> entries = new ArrayList<>();

        for (Map.Entry<Long, AppUser> entry : userMap.entrySet()) {
            Long userId = entry.getKey();
            AppUser user = entry.getValue();

            int gamesPlayed = userRoomsPlayed.getOrDefault(userId, Collections.emptySet()).size();
            int totalWins = userTotalWins.getOrDefault(userId, 0);
            Map<String, Integer> breakdown = userWinBreakdown.getOrDefault(userId, Collections.emptyMap());

            int fullHouseWins = breakdown.getOrDefault("FULL_HOUSE", 0);
            int earlyFiveWins = breakdown.getOrDefault("EARLY_FIVE", 0);
            int cornersWins = breakdown.getOrDefault("FOUR_CORNERS", 0);
            int topLineWins = breakdown.getOrDefault("TOP_LINE", 0);
            int midLineWins = breakdown.getOrDefault("MIDDLE_LINE", 0);
            int btmLineWins = breakdown.getOrDefault("BOTTOM_LINE", 0);

            int points = (gamesPlayed * 5)
                    + (fullHouseWins * 50)
                    + (earlyFiveWins * 20)
                    + (cornersWins * 15)
                    + (topLineWins * 15)
                    + (midLineWins * 15)
                    + (btmLineWins * 15);

            List<String> badges = new ArrayList<>();
            if (fullHouseWins > 0) badges.add("👑 Full House Champion");
            if (earlyFiveWins > 0) badges.add("⚡ Early Bird");
            if ((topLineWins + midLineWins + btmLineWins) >= 2) badges.add("📐 Line Master");
            if (cornersWins > 0) badges.add("🎯 Corner Ace");
            if (gamesPlayed >= 5) badges.add("🌟 Tambola Veteran");
            if (totalWins >= 3) badges.add("🔥 Hot Streak");

            String flatDetails = null;
            if (flatMembershipRepository != null) {
                try {
                    var memberships = flatMembershipRepository.findByUserId(userId);
                    if (memberships != null && !memberships.isEmpty()) {
                        var fm = memberships.get(0);
                        if (fm.getFlat() != null) {
                            String fNum = fm.getFlat().getFlatNumber();
                            String tBlock = fm.getFlat().getTowerBlock();
                            flatDetails = (tBlock != null && !tBlock.isBlank() ? tBlock + " - " : "") + fNum;
                        }
                    }
                } catch (Exception e) {
                    // Ignore gracefully
                }
            }

            entries.add(LeaderboardEntry.builder()
                    .userId(userId)
                    .userName(user.getFullName() != null ? user.getFullName() : "Resident")
                    .avatarUrl(user.getProfilePicUrl())
                    .flatDetails(flatDetails)
                    .totalGamesPlayed(gamesPlayed)
                    .totalWins(totalWins)
                    .totalPoints(points)
                    .winBreakdown(breakdown)
                    .badges(badges)
                    .build());
        }

        entries.sort(Comparator.comparingInt(LeaderboardEntry::getTotalPoints)
                .thenComparingInt(LeaderboardEntry::getTotalWins)
                .thenComparingInt(LeaderboardEntry::getTotalGamesPlayed)
                .reversed());

        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        return CircleLeaderboardResponse.builder()
                .circleId(circleId)
                .circleName(circle.getCircleName())
                .totalGamesInCircle(rooms.size())
                .entries(entries)
                .build();
    }
}
