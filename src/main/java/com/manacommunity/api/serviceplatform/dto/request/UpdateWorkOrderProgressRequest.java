package com.manacommunity.api.serviceplatform.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateWorkOrderProgressRequest {
    @Size(max = 4000)
    private String notes;
    private String checklistItems;
    private String materialsUsed;
    private String beforePhotos;
    private String afterPhotos;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
}
