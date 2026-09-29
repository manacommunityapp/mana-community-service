package com.manacommunity.api.dto.admin;

import com.manacommunity.api.model.ContentReport;
import lombok.Builder;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
@Builder
public class ReportResponse {
    private Long   id;
    private Long   reporterId;
    private String reporterName;
    private String targetType;
    private Long   targetId;
    private String targetContent;
    private String targetAuthor;
    private String reason;
    private String status;
    private String createdAt;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static ReportResponse from(ContentReport r) {
        return ReportResponse.builder()
                .id(r.getId())
                .reporterId(r.getReporter().getId())
                .reporterName(r.getReporter().getFullName())
                .targetType(r.getTargetType())
                .targetId(r.getTargetId())
                .targetContent(r.getTargetContent())
                .targetAuthor(r.getTargetAuthor())
                .reason(r.getReason())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().format(FMT) : null)
                .build();
    }
}
