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
                .reporterId(r.getReportedBy() != null ? r.getReportedBy().getId() : null)
                .reporterName(r.getReportedBy() != null ? r.getReportedBy().getFullName() : null)
                .targetType(r.getContentType())
                .targetId(r.getContentId())
                .targetContent(r.getDescription())
                .targetAuthor(null)
                .reason(r.getReason())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().format(FMT) : null)
                .build();
    }
}
