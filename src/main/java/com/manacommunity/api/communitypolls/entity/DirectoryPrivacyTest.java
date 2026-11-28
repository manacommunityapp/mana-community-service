package com.manacommunity.api.communitypolls.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "directory_privacy_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DirectoryPrivacyTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private PrivacyScenario scenario;

    private String fieldName;
    @Enumerated(EnumType.STRING)
    private VisibilityLevel configuredVisibility;

    private boolean visibleInNeighborSearch;
    private boolean visibleInMcDashboard;
    private boolean visibleInClubDirectory;
    private boolean leakedInJsonResponse;
    private String leakedEndpoint;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum PrivacyScenario {
        PHONE_HIDDEN_FROM_NEIGHBORS, VEHICLE_VISIBLE_TO_MC_ONLY,
        HOBBY_CLUB_MEMBERS_ONLY, FIELD_LEAKED_IN_SEARCH_API,
        FIELD_LEAKED_IN_PROFILE_API, BULK_EXPORT_PRIVACY_CHECK,
        GUEST_VIEW_NO_PERSONAL_DATA, MC_EXPORT_INCLUDES_ALL
    }

    public enum VisibilityLevel { PUBLIC, NEIGHBORS_ONLY, MC_ONLY, CLUB_ONLY, HIDDEN }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
