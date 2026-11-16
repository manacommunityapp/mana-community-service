package com.manacommunity.api.hrm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDesignationRequest {

    @NotBlank
    private String designationName;

    private Integer level;
}
