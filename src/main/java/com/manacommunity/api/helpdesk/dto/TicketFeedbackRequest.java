package com.manacommunity.api.helpdesk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketFeedbackRequest {
    @NotNull
    @Min(1)
    @Max(5)
    private Integer satisfactionRating;
    private String feedbackRemarks;
    private boolean signOffConfirmed;
}
