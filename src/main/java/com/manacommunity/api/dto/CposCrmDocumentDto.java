package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CposCrmDocumentDto {
    private Long id;
    private Long leadId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private String uploadedBy;
    private LocalDateTime createdAt;
}
