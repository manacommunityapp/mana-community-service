package com.cpn.domain.resume.model;

import com.cpn.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "resumes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume extends TenantAwareEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String resumeName;

    private Integer version;
    private String fileUrl;
    private Boolean isPrimary;
    private Double atsScore;
    private Boolean isAiGenerated;
}
