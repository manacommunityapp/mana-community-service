package com.manacommunity.api.datamigration.controller;

import com.manacommunity.api.datamigration.entity.*;
import com.manacommunity.api.datamigration.entity.BulkOnboardingTest.*;
import com.manacommunity.api.datamigration.entity.AppUpgradeTest.*;
import com.manacommunity.api.datamigration.entity.LocalizationTest.*;
import com.manacommunity.api.datamigration.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/data-migration-test")
@RequiredArgsConstructor
public class DataMigrationTestController {

    private final BulkOnboardingTestRepository onboardingRepo;
    private final AppUpgradeTestRepository upgradeRepo;
    private final LocalizationTestRepository l10nRepo;

    public record MigrationDashboard(
            long totalTests, long passed, long failed, long pending, double passRate,
            List<BulkOnboardingTest> onboardingTests,
            List<AppUpgradeTest> upgradeTests,
            List<LocalizationTest> localizationTests
    ) {}

    @GetMapping("/dashboard")
    public ResponseEntity<MigrationDashboard> getDashboard() {
        var onboardings = onboardingRepo.findAll();
        var upgrades = upgradeRepo.findAll();
        var l10ns = l10nRepo.findAll();

        List<TestResult> all = new ArrayList<>();
        onboardings.forEach(o -> all.add(o.getTestResult()));
        upgrades.forEach(u -> all.add(u.getTestResult()));
        l10ns.forEach(l -> all.add(l.getTestResult()));

        long passed = all.stream().filter(r -> r == TestResult.PASS).count();
        long failed = all.stream().filter(r -> r == TestResult.FAIL).count();
        long pending = all.stream().filter(r -> r == TestResult.PENDING).count();
        double passRate = all.isEmpty() ? 0 : (double) passed / all.size() * 100;

        return ResponseEntity.ok(new MigrationDashboard(all.size(), passed, failed, pending, passRate,
                onboardings, upgrades, l10ns));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<Map<String, Object>> seedDemo() {
        onboardingRepo.deleteAll(); upgradeRepo.deleteAll(); l10nRepo.deleteAll();

        onboardingRepo.saveAll(List.of(
                BulkOnboardingTest.builder().communityId("COMM-001").testCaseCode("CSV-001").testCaseName("Clean 5,000-row CSV import — all valid")
                        .scenario(OnboardingScenario.CLEAN_CSV_5000_ROWS).csvFileName("greenvalley_residents_5000.csv")
                        .totalRows(5000).validRows(5000).invalidRows(0).duplicateRows(0)
                        .malformedPhoneNumbers(0).duplicateFlats(0).specialCharacterIssues(0).missingMandatoryFields(0)
                        .importDurationMs(12400).rollbackOnError(false).rowsImported(5000).rowsSkipped(0)
                        .testResult(TestResult.PASS).build(),

                BulkOnboardingTest.builder().communityId("COMM-001").testCaseCode("CSV-002").testCaseName("Malformed phone numbers — 12 invalid formats")
                        .scenario(OnboardingScenario.MALFORMED_PHONE_NUMBERS).csvFileName("prestige_residents_mixed.csv")
                        .totalRows(3200).validRows(3188).invalidRows(12).duplicateRows(0)
                        .malformedPhoneNumbers(12).duplicateFlats(0).specialCharacterIssues(0).missingMandatoryFields(0)
                        .importDurationMs(8900).rollbackOnError(false).rowsImported(3188).rowsSkipped(12)
                        .testResult(TestResult.PASS).sampleErrors("+91-98765-4321 (10 digits), 098765432100 (12 digits), abc-phone (non-numeric), +1-555-1234 (non-Indian)").build(),

                BulkOnboardingTest.builder().communityId("COMM-001").testCaseCode("CSV-003").testCaseName("Duplicate flats — 45 flat numbers appear twice")
                        .scenario(OnboardingScenario.DUPLICATE_FLATS).csvFileName("brigade_residents_dupes.csv")
                        .totalRows(4800).validRows(4710).invalidRows(45).duplicateRows(45)
                        .malformedPhoneNumbers(0).duplicateFlats(45).specialCharacterIssues(0).missingMandatoryFields(0)
                        .importDurationMs(11200).rollbackOnError(false).rowsImported(4755).rowsSkipped(45)
                        .testResult(TestResult.FAIL).failureReason("Duplicate flat A-305 imported twice — second owner overwrote first. Unique constraint not enforced on (community_id, flat_number).").build(),

                BulkOnboardingTest.builder().communityId("COMM-001").testCaseCode("CSV-004").testCaseName("Special characters — Telugu/Kannada names in UTF-8")
                        .scenario(OnboardingScenario.SPECIAL_CHARACTERS_UTF8).csvFileName("hyderabad_residents_telugu.csv")
                        .totalRows(2500).validRows(2492).invalidRows(8).duplicateRows(0)
                        .malformedPhoneNumbers(0).duplicateFlats(0).specialCharacterIssues(8).missingMandatoryFields(0)
                        .importDurationMs(7200).rollbackOnError(false).rowsImported(2492).rowsSkipped(8)
                        .testResult(TestResult.FAIL).failureReason("8 names with Telugu diacritics (e.g., శ్రీనివాస్ రావు) stored as mojibake. CSV opened as ISO-8859-1 instead of UTF-8.").build()
        ));

        upgradeRepo.saveAll(List.of(
                AppUpgradeTest.builder().communityId("COMM-001").testCaseCode("UPG-001").testCaseName("Clean upgrade v5.1 → v5.2 — no pending queue")
                        .scenario(UpgradeScenario.CLEAN_UPGRADE).fromVersion("5.1.0").toVersion("5.2.0")
                        .deviceModel("Samsung Galaxy Tab A8").platform("Android 13")
                        .sqliteRecordsBefore(8500).sqliteRecordsAfter(8500).dataLoss(false).recordsLost(0)
                        .offlineQueuePreserved(true).queuedItemsBefore(0).queuedItemsAfter(0).queuedItemsSyncedPostUpgrade(0)
                        .schemaMigrationSuccessful(true).upgradeDurationMs(4500)
                        .testResult(TestResult.PASS).build(),

                AppUpgradeTest.builder().communityId("COMM-001").testCaseCode("UPG-002").testCaseName("Upgrade with 15 pending offline entries — queue preserved")
                        .scenario(UpgradeScenario.UPGRADE_WITH_PENDING_OFFLINE_QUEUE).fromVersion("5.1.0").toVersion("5.2.0")
                        .deviceModel("Samsung Galaxy A14").platform("Android 14")
                        .sqliteRecordsBefore(6200).sqliteRecordsAfter(6200).dataLoss(false).recordsLost(0)
                        .offlineQueuePreserved(true).queuedItemsBefore(15).queuedItemsAfter(15).queuedItemsSyncedPostUpgrade(15)
                        .schemaMigrationSuccessful(true).upgradeDurationMs(6800)
                        .testResult(TestResult.PASS).build(),

                AppUpgradeTest.builder().communityId("COMM-001").testCaseCode("UPG-003").testCaseName("Upgrade with schema change — new visitor_photo column")
                        .scenario(UpgradeScenario.UPGRADE_WITH_SCHEMA_CHANGE).fromVersion("5.0.0").toVersion("5.2.0")
                        .deviceModel("iPad 10th Gen").platform("iOS 17.1")
                        .sqliteRecordsBefore(4100).sqliteRecordsAfter(4098).dataLoss(true).recordsLost(2)
                        .dataLossDetail("2 visitor records with binary photo blobs lost during ALTER TABLE migration. Photos stored inline instead of as file references.")
                        .offlineQueuePreserved(true).queuedItemsBefore(3).queuedItemsAfter(3).queuedItemsSyncedPostUpgrade(3)
                        .schemaMigrationSuccessful(true).upgradeDurationMs(18500)
                        .testResult(TestResult.FAIL).failureReason("2 records lost: inline BLOB photos exceeded SQLite page size during ALTER TABLE. Need to migrate blobs to file references before schema change.").build()
        ));

        l10nRepo.saveAll(List.of(
                LocalizationTest.builder().communityId("COMM-001").testCaseCode("L10N-001").testCaseName("Telugu — button text truncation on 5-inch screen")
                        .scenario(LocalizationScenario.VERNACULAR_TEXT_TRUNCATION).language("Telugu").languageCode("te")
                        .screenName("Visitor Approval Screen").deviceModel("Redmi Note 12").screenWidthDp(360).fontScale(1.0)
                        .textTruncated(true).truncatedElement("'అనుమతించండి' (Approve) button truncated to 'అనుమతి...'")
                        .buttonWrapped(false).layoutBroken(false)
                        .audioPromptTested(false).rtlLayoutCorrect(true)
                        .totalStrings(240).translatedStrings(238).missingTranslations(2)
                        .testResult(TestResult.FAIL).failureReason("Telugu 'Approve' button text truncated on 360dp screen. Need min-width or text scaling.").build(),

                LocalizationTest.builder().communityId("COMM-001").testCaseCode("L10N-002").testCaseName("Hindi — large font scale 1.5× overflow")
                        .scenario(LocalizationScenario.LARGE_FONT_SCALE_OVERFLOW).language("Hindi").languageCode("hi")
                        .screenName("Maintenance Bill Summary").deviceModel("Samsung Galaxy A14").screenWidthDp(384).fontScale(1.5)
                        .textTruncated(false).buttonWrapped(true).wrappedButtonLabel("'भुगतान करें' (Pay Now) wraps to 2 lines at 1.5× scale")
                        .layoutBroken(true).layoutIssueDescription("Bill total and Pay Now button overlap at 1.5× font scale — needs ScrollView or min-height constraint")
                        .audioPromptTested(false).rtlLayoutCorrect(true)
                        .totalStrings(240).translatedStrings(240).missingTranslations(0)
                        .testResult(TestResult.FAIL).failureReason("Layout overlap at 1.5× font scale: bill total and payment button collide").build(),

                LocalizationTest.builder().communityId("COMM-001").testCaseCode("L10N-003").testCaseName("Kannada — audio voice prompt at gate intercom")
                        .scenario(LocalizationScenario.AUDIO_VOICE_PROMPT).language("Kannada").languageCode("kn")
                        .screenName("Gate Intercom").deviceModel("Guard Tablet").screenWidthDp(800).fontScale(1.0)
                        .textTruncated(false).buttonWrapped(false).layoutBroken(false)
                        .audioPromptTested(true).audioPromptPlayed(true).audioPromptLanguage("Kannada").audioPromptDurationMs(4200)
                        .rtlLayoutCorrect(true).totalStrings(60).translatedStrings(60).missingTranslations(0)
                        .testResult(TestResult.PASS).build(),

                LocalizationTest.builder().communityId("COMM-001").testCaseCode("L10N-004").testCaseName("Tamil — missing translations on Complaint screen")
                        .scenario(LocalizationScenario.MISSING_TRANSLATIONS).language("Tamil").languageCode("ta")
                        .screenName("Complaint Registration").deviceModel("Redmi Note 11").screenWidthDp(360).fontScale(1.0)
                        .textTruncated(false).buttonWrapped(false).layoutBroken(false)
                        .audioPromptTested(false).rtlLayoutCorrect(true)
                        .totalStrings(85).translatedStrings(72).missingTranslations(13)
                        .testResult(TestResult.FAIL).failureReason("13 strings display English fallback on Tamil screen: 'Priority', 'Attachment', 'Category', 'Sub-category' and 9 error messages untranslated").build(),

                LocalizationTest.builder().communityId("COMM-001").testCaseCode("L10N-005").testCaseName("Marathi — currency format ₹ with Devanagari numerals option")
                        .scenario(LocalizationScenario.CURRENCY_FORMAT).language("Marathi").languageCode("mr")
                        .screenName("Payment Summary").deviceModel("Samsung Galaxy M14").screenWidthDp(384).fontScale(1.0)
                        .textTruncated(false).buttonWrapped(false).layoutBroken(false)
                        .audioPromptTested(false).rtlLayoutCorrect(true)
                        .totalStrings(45).translatedStrings(45).missingTranslations(0)
                        .testResult(TestResult.PASS).build()
        ));

        return ResponseEntity.ok(Map.of("status", "seeded", "onboardingTests", 4, "upgradeTests", 3, "localizationTests", 5));
    }
}
