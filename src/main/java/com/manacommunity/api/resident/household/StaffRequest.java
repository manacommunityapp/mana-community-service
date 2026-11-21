package com.manacommunity.api.resident.household;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StaffRequest {

    @NotBlank
    @Size(max = 200)
    private String staffName;

    @NotBlank
    @Size(max = 30)
    private String staffType; // MAID, COOK, DRIVER, NANNY, GARDENER, OTHER

    @Size(max = 20)
    private String phone;

    private LocalDate sinceDate;
}
