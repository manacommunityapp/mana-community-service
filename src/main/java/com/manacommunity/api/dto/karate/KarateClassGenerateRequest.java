package com.manacommunity.api.dto.karate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class KarateClassGenerateRequest {

    @NotNull
    private LocalDate fromDate;

    @NotNull
    private LocalDate toDate;
}
