package com.manacommunity.api.datamigration.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dm_localization_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LocalizationTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private LocalizationScenario scenario;

    private String language;
    private String languageCode;
    private String screenName;
    private String deviceModel;
    private int screenWidthDp;
    private double fontScale;

    private boolean textTruncated;
    private String truncatedElement;
    private boolean buttonWrapped;
    private String wrappedButtonLabel;
    private boolean layoutBroken;
    private String layoutIssueDescription;

    private boolean audioPromptTested;
    private boolean audioPromptPlayed;
    private String audioPromptLanguage;
    private int audioPromptDurationMs;

    private boolean rtlLayoutCorrect;
    private int totalStrings;
    private int translatedStrings;
    private int missingTranslations;

    @Enumerated(EnumType.STRING)
    private BulkOnboardingTest.TestResult testResult;

    private String failureReason;

    public enum LocalizationScenario {
        VERNACULAR_TEXT_TRUNCATION, BUTTON_WRAP_SMALL_SCREEN,
        LARGE_FONT_SCALE_OVERFLOW, AUDIO_VOICE_PROMPT,
        RTL_LAYOUT_CHECK, MISSING_TRANSLATIONS, CURRENCY_FORMAT
    }
}
