package com.manacommunity.api.communitypolls.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "classifieds_moderation_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClassifiedsModerationTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private ClassifiedScenario scenario;

    @Enumerated(EnumType.STRING)
    private GeoScope geoScope;

    private String postTitle;
    private boolean imageUploaded;
    private long imageOriginalSizeBytes;
    private long imageCompressedSizeBytes;
    private double compressionRatio;

    private boolean profanityDetected;
    private String detectedProfanity;
    private boolean phishingLinkDetected;
    private String detectedLink;
    private boolean postBlocked;
    private boolean autoModerated;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum ClassifiedScenario {
        SOCIETY_ONLY_VISIBILITY, NEARBY_3KM_VISIBILITY, IMAGE_COMPRESSION_CHECK,
        PROFANITY_BLOCKED, PHISHING_LINK_BLOCKED, CROSS_SOCIETY_VISIBILITY_LEAK,
        EXPIRED_POST_AUTO_ARCHIVE, SPAM_REPEAT_POST_THROTTLE
    }

    public enum GeoScope { SOCIETY_ONLY, NEARBY_3KM, CITY_WIDE }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
