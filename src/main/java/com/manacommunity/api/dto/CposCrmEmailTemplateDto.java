package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CposCrmEmailTemplateDto {
    private Long id;
    private String name;
    private String subject;
    private String body;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
