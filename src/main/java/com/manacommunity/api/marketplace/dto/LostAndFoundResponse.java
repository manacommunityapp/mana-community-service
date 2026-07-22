package com.manacommunity.api.marketplace.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LostAndFoundResponse {

    private Long id;
    private String title;
    private String description;
    private String type;
    private String category;
    private String imageUrl;
    private String location;
    private LocalDate dateOccurred;
    private String status;
    private ReporterRef reporter;
    private Long communityId;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class ReporterRef {
        private Long id;
        private String fullName;
    }
}
