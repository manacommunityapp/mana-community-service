package com.manacommunity.api.datamigration.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dm_bulk_onboarding_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BulkOnboardingTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private OnboardingScenario scenario;

    private String csvFileName;
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private int duplicateRows;

    private int malformedPhoneNumbers;
    private int duplicateFlats;
    private int specialCharacterIssues;
    private int missingMandatoryFields;

    private long importDurationMs;
    private boolean rollbackOnError;
    private int rowsImported;
    private int rowsSkipped;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;

    private String failureReason;
    private String sampleErrors;

    public enum OnboardingScenario {
        CLEAN_CSV_5000_ROWS, MALFORMED_PHONE_NUMBERS, DUPLICATE_FLATS,
        SPECIAL_CHARACTERS_UTF8, MISSING_MANDATORY_FIELDS,
        MIXED_ERRORS_PARTIAL_IMPORT, EMPTY_CSV, OVERSIZED_CSV_50K_ROWS
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
