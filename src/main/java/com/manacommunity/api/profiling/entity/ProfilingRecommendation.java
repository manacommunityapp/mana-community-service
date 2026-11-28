package com.manacommunity.api.profiling.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "profiling_recommendations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfilingRecommendation extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceId;

    @Enumerated(EnumType.STRING)
    private RuntimeDomain domain;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 2000)
    private String codeSnippet;

    @Enumerated(EnumType.STRING)
    private RecommendationStatus status;

    private String estimatedImpact;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;

    public enum RuntimeDomain { JVM_GC, JVM_THREADS, NODEJS_EVENT_LOOP, NODEJS_MEMORY, BROWSER_MEMORY, BROWSER_DOM }
    public enum Severity { CRITICAL, HIGH, MEDIUM, LOW, INFO }
    public enum RecommendationStatus { OPEN, IN_PROGRESS, RESOLVED, WONT_FIX }
}
