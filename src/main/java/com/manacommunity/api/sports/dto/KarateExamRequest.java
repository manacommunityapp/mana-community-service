package com.manacommunity.api.sports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class KarateExamRequest {

    @NotBlank
    @Size(max = 150)
    private String examTitle;

    @NotNull
    private LocalDate examDate;

    @NotNull
    private Long targetBeltId;

    @Size(max = 500)
    private String notes;
}
