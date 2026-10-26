package com.manacommunity.api.sports.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KarateExamResultRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String result;       // PASS, FAIL, ABSENT

    @Min(0) @Max(100)
    private Integer score;

    private String examinerNotes;
    private Boolean awardBelt;   // true → promote student to exam's targetBelt
}
