package com.manacommunity.api.sports.dto;

import com.manacommunity.api.sports.model.StreamStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SportsStreamResponse {

    private Long matchId;
    private StreamStatus status;
    private String title;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private List<CameraDto> cameras;
    private List<MessageDto> messages;

    @Data
    @Builder
    public static class CameraDto {
        private Long id;
        private String label;
        private String url;
        private String embedUrl;
        private String platform;
        private boolean active;
    }

    @Data
    @Builder
    public static class MessageDto {
        private Long id;
        private String authorName;
        private String text;
        private boolean commentary;
        private LocalDateTime postedAt;
    }
}
