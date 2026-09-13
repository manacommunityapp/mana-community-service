package com.manacommunity.api.dto.karate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class KarateExamResultEntry {

    @NotNull
    private Long enrollmentId;

    @NotNull
    private Boolean passed;

    private Long newBeltId;

    private BigDecimal score;

    private String remarks;
}
