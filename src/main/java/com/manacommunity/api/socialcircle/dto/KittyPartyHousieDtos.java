package com.manacommunity.api.socialcircle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class KittyPartyHousieDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRoomRequest {
        @NotBlank(message = "Room name is required")
        private String roomName;

        private Integer maxPlayers;

        private Boolean allowGuestPlayers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameRoomResponse {
        private Long id;
        private Long circleId;
        private String roomName;
        private String gameType;
        private String status;
        private String roomCode;
        private Boolean allowGuestPlayers;
        private Integer maxPlayers;
        private Long activePlayersCount;
        private Long createdById;
        private String createdByName;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private Integer lastCalledNumber;
        private List<Integer> calledNumbers;
        private List<WinnerClaimResponse> winners;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinByCodeRequest {
        @NotBlank(message = "Room code is required")
        private String roomCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketResponse {
        private Long ticketId;
        private Long roomId;
        private Long playerId;
        private List<List<Integer>> matrix; // 3 rows x 9 cols
        private String ticketHash;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallNumberResponse {
        private Long roomId;
        private Integer sequenceNum;
        private Integer calledNumber;
        private LocalDateTime calledAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClaimPrizeRequest {
        @NotNull(message = "Ticket ID is required")
        private Long ticketId;

        @NotBlank(message = "Claim type is required")
        private String claimType; // EARLY_FIVE, FOUR_CORNERS, TOP_LINE, MIDDLE_LINE, BOTTOM_LINE, FULL_HOUSE
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WinnerClaimResponse {
        private Long id;
        private Long roomId;
        private Long ticketId;
        private Long userId;
        private String userName;
        private String claimType;
        private String claimStatus;
        private LocalDateTime claimedAt;
    }
}
