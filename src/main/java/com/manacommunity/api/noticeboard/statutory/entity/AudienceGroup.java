package com.manacommunity.api.noticeboard.statutory.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notice_audience_groups")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AudienceGroup extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String name;
    private String description;

    private boolean dynamic;
    private boolean systemDefined;

    @Column(columnDefinition = "TEXT")
    private String ruleExpression;

    private int cachedMemberCount;
    private LocalDateTime lastEvaluatedAt;

    @Enumerated(EnumType.STRING)
    private GroupStatus groupStatus;

    public enum GroupStatus { ACTIVE, ARCHIVED, EVALUATING }
}
