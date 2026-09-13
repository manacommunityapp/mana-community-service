package com.manacommunity.api.dto.karate;

import com.manacommunity.api.model.karate.SportsKarateProgram;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class KarateProgramRequest {

    @NotBlank
    private String name;

    @NotNull
    private Long sportId;

    private Long instructorUserId;

    private SportsKarateProgram.ProgramLevel level;

    private Integer minAge;
    private Integer maxAge;
    private BigDecimal monthlyFee;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxStudents;
    private Boolean active;
    private String description;

    private Long communityId;
}
