package com.manacommunity.api.sports.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveReviewRequest {

    @NotBlank(message = "Action is required")
    private String action; // "APPROVE", "REJECT", "LINK_USER", "OVERRIDE"

    private Long resolvedUserId;
    private Long overrideCategoryId;
    private String overrideMatchType;
    private String reviewNotes;
}
