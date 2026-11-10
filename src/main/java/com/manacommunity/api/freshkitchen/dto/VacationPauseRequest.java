package com.manacommunity.api.freshkitchen.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacationPauseRequest {

    @NotNull(message = "pauseStartDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate pauseStartDate;

    @NotNull(message = "pauseEndDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate pauseEndDate;

    private String reason;
}
