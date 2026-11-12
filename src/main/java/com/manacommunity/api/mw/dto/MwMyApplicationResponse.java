package com.manacommunity.api.mw.dto;

import com.manacommunity.api.mw.entity.MwJobApplication;
import com.manacommunity.api.mw.entity.MwJobListing;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MwMyApplicationResponse {

    private Long applicationId;
    private String status;
    private LocalDateTime appliedAt;
    private LocalDateTime respondedAt;
    private String employerNote;
    private Long jobId;
    private String jobTitle;
    private String jobLocation;
    private Boolean jobIsRemote;
    private String employmentType;

    public static MwMyApplicationResponse from(MwJobApplication app) {
        MwJobListing job = app.getJob();
        return MwMyApplicationResponse.builder()
                .applicationId(app.getId())
                .status(app.getStatus())
                .appliedAt(app.getAppliedAt())
                .respondedAt(app.getRespondedAt())
                .employerNote(app.getEmployerNote())
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : null)
                .jobLocation(job != null ? job.getLocation() : null)
                .jobIsRemote(job != null ? job.getIsRemote() : null)
                .employmentType(job != null ? job.getEmploymentType() : null)
                .build();
    }
}
