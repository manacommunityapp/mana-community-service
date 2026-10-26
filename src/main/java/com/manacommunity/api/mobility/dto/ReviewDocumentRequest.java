package com.manacommunity.api.mobility.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewDocumentRequest {
    @NotNull  private Boolean approved;
              private String rejectReason;
}
