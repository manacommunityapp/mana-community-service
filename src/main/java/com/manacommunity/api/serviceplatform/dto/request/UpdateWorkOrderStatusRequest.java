package com.manacommunity.api.serviceplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateWorkOrderStatusRequest {
    @NotBlank
    private String status;
    private String notes;
    private String checklistItems;
    private String materialsUsed;
    private String beforePhotos;
    private String afterPhotos;
}
