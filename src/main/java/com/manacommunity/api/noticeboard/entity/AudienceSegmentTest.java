package com.manacommunity.api.noticeboard.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audience_segment_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AudienceSegmentTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private SegmentScenario scenario;

    private String segmentName;
    private String segmentCriteria;
    private int expectedRecipients;
    private int actualRecipients;
    private boolean towerFilter;
    private boolean wingFilter;
    private boolean floorRangeFilter;
    private boolean ownerTenantFilter;
    private boolean languageFilter;
    private String noticeLanguage;
    private boolean translationAccurate;
    private boolean excludeListApplied;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum SegmentScenario {
        TOWER_SPECIFIC_PLUMBING_NOTICE, OWNER_ONLY_AGM_VOTING,
        TENANT_MAINTENANCE_STAFF_ENTRY, FLOOR_RANGE_ELEVATOR_SHUTDOWN,
        MULTILINGUAL_HINDI_NOTICE, EXCLUDE_VACATED_FLATS,
        SEGMENT_OVERLAP_DOUBLE_DELIVERY, ALL_RESIDENTS_BROADCAST
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
