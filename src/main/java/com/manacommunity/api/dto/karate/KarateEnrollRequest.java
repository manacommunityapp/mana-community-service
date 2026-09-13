package com.manacommunity.api.dto.karate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KarateEnrollRequest {

    @NotNull
    private Long studentUserId;

    private Long initialBeltId;

    private String notes;
}
