package com.manacommunity.api.dto.karate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class KarateGradingExamRequest {

    @NotNull
    private Long targetBeltId;

    @NotNull
    private LocalDate scheduledDate;

    private Long venueId;
    private String examinerName;
    private Integer maxCandidates;
    private String notes;
}
