package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CposCrmLeadDto {
    private Long id;
    private String leadName;

    @Data @Builder
    public static class TagDto {
        private Long id;
        private String name;
        private String color;
    }
    private String email;
    private String phone;
    private String source;
    private String stage;
    private String leadType;
    private Integer budgetLakhs;
    private String budgetDisplay;
    private String assignedTo;
    private Integer daysInStage;
    private Integer daysTotal;
    private Integer aiScore;
    private LocalDateTime followUpAt;
    private String followUpNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TagDto> tags;
    private List<CposCrmActivityDto> activities;
    private String approvalStatus;
    private String approvalReason;
    private String approvalComment;
}
