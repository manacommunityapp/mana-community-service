package com.manacommunity.api.wellnessbasket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessPauseRequest {

    @NotNull(message = "Pause start date is required")
    private LocalDate pauseFrom;

    @NotNull(message = "Pause end date is required")
    private LocalDate pauseUntil;

    private String reason;
}
