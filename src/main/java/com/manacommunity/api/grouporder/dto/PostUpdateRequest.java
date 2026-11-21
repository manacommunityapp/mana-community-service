package com.manacommunity.api.grouporder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostUpdateRequest {

    @NotBlank
    private String message;

    @NotBlank @Size(max = 50)
    private String updateType;
}
