package com.manacommunity.api.dto.admin;

import com.manacommunity.api.model.Announcement;
import lombok.Builder;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
@Builder
public class AnnouncementResponse {
    private Long    id;
    private String  title;
    private String  content;
    private String  priority;
    private boolean pinned;
    private Long    authorId;
    private String  authorName;
    private String  createdAt;
    private String  expiresAt;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static AnnouncementResponse from(Announcement a) {
        return AnnouncementResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .priority(a.getPriority())
                .pinned(Boolean.TRUE.equals(a.getPinned()))
                .authorId(a.getAuthor().getId())
                .authorName(a.getAuthor().getFullName())
                .createdAt(a.getCreatedAt() != null ? a.getCreatedAt().format(FMT) : null)
                .expiresAt(a.getExpiresAt() != null ? a.getExpiresAt().format(FMT) : null)
                .build();
    }
}
