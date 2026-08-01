package com.cpn.domain.jobs.model;

import com.cpn.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "job_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication extends TenantAwareEntity {

    @Column(nullable = false)
    private UUID jobId;

    @Column(nullable = false)
    private UUID applicantId;

    private String applicantName;
    private String resumeUrl;
    private Integer matchScore;
    private String status; // APPLIED, SHORTLISTED, REJECTED, HIRED
}
