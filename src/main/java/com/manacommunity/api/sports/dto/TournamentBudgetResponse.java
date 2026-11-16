package com.manacommunity.api.sports.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TournamentBudgetResponse {

    private Long id;
    private Long tournamentId;
    private String tournamentName;
    private Long communityId;
    /** Total amount budgeted to spend on running the tournament. */
    private BigDecimal totalPlannedAmount;
    /** Total registration fees collected from players (income). */
    private BigDecimal totalCollectedAmount;
    /** Surplus when collected > planned; negative means under-funded. */
    private BigDecimal variance;
    private String currency;
    private String notes;
    private List<LineItemResponse> lineItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class LineItemResponse {
        private Long id;
        private String category;
        private String description;
        private BigDecimal plannedAmount;
        private LocalDateTime createdAt;
    }
}
