package com.cpn.domain.ai.model;

import com.cpn.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "ai_insights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInsight extends TenantAwareEntity {

    @Column(nullable = false)
    private UUID userId;

    private String promptCategory; // RESUME_REVIEW, SALARY_BENCHMARK, SKILL_GAP, INTERVIEW_PREP

    @Column(length = 4000)
    private String promptText;

    @Column(length = 4000)
    private String responseText;
}
