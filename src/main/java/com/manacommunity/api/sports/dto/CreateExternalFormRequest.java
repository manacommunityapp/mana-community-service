package com.manacommunity.api.sports.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExternalFormRequest {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @Builder.Default
    private String provider = "GOOGLE_FORMS"; // "GOOGLE_FORMS", "MANA_FORM"

    private String title;
    private String description;

    @Builder.Default
    private String approvalMode = "TOURNAMENT_DEFAULT"; // "TOURNAMENT_DEFAULT", "AUTO", "MANUAL"

    private List<String> fieldKeys;

    private String googleAccessToken;
}
