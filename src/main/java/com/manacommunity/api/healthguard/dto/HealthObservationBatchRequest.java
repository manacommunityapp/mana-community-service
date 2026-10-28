package com.manacommunity.api.healthguard.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class HealthObservationBatchRequest {
    @NotBlank
    private String provider;

    @NotEmpty
    @Size(max = 500)
    @Valid
    private List<Observation> observations;

    @Data
    public static class Observation {
        @NotBlank
        @Size(max = 160)
        private String sourceRecordId;

        @NotBlank
        private String type;

        @Size(max = 160)
        private String sourceApp;

        private BigDecimal numericValue;

        @Size(max = 32)
        private String unit;

        @NotNull
        private OffsetDateTime startedAt;

        @NotNull
        private OffsetDateTime endedAt;
    }
}
