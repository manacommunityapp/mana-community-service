package com.manacommunity.api.onboarding.ingestion;

import com.manacommunity.api.onboarding.ingestion.model.BulkOnboardingJobDto;
import com.manacommunity.api.onboarding.ingestion.model.IngestionValidationReportDto;
import com.manacommunity.api.onboarding.ingestion.model.OnboardingRowRecord;
import com.manacommunity.api.onboarding.ingestion.service.BulkOnboardingIngestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BulkOnboardingIngestionServiceTest {

    private BulkOnboardingIngestionServiceImpl service;
    private final Long communityId = 4001L;

    @BeforeEach
    void setUp() {
        service = new BulkOnboardingIngestionServiceImpl();
    }

    @Test
    @DisplayName("Should sanitize dirty phone formats, flat numbers, and opening balances in dry-run mode")
    void testDryRunParsingAndNormalization() {
        String csvContent = "Tower/Flat,Resident_Name,Phone,Occupancy_Type,Vehicle_Number,Opening_Balance\n" +
                "Tower A - 101,Rahul Sharma,+91 98765-43210,OWNER,KA-01-MJ-1001,12000 CR\n" +
                "B-202,Priya Patel,09876543211,TENANT,KA-02-AB-2002,4500 DR\n" +
                "Block C 303,Amit Verma,9876543212,OWNER,,₹ 0\n";

        InputStream stream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        IngestionValidationReportDto report = service.parseAndValidateCsv(communityId, "messy_residents.csv", stream, true);

        assertNotNull(report);
        assertEquals(3, report.getTotalRows());
        assertEquals(3, report.getCleanRowCount());
        assertEquals(0, report.getFatalErrorRowCount());
        assertTrue(report.isReadyForCommit());

        // Net opening ledger: +12000 - 4500 + 0 = 7500.00
        assertEquals(new BigDecimal("7500"), report.getOpeningLedgerSum());

        // Verify job status
        BulkOnboardingJobDto job = service.getJobStatus(report.getJobId());
        assertEquals(BulkOnboardingJobDto.Status.DRY_RUN_COMPLETED, job.getStatus());
    }

    @Test
    @DisplayName("Should detect duplicate primary owners for same flat and flag warnings")
    void testDuplicateOwnerDetection() {
        String csvContent = "Tower/Flat,Resident_Name,Phone,Occupancy_Type,Vehicle_Number,Opening_Balance\n" +
                "Tower A - 101,Suresh Kumar,9876543210,OWNER,,0\n" +
                "Tower A - 101,Ramesh Kumar,9876543211,OWNER,,0\n"; // Duplicate owner for same flat

        InputStream stream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        IngestionValidationReportDto report = service.parseAndValidateCsv(communityId, "duplicate_owners.csv", stream, true);

        assertNotNull(report);
        assertEquals(2, report.getTotalRows());
        assertEquals(1, report.getCleanRowCount());
        assertEquals(1, report.getWarningRowCount());
        assertEquals(0, report.getFatalErrorRowCount());
        assertTrue(report.isReadyForCommit()); // Warnings do not block commit

        OnboardingRowRecord warningRow = report.getSampleErrors().get(0);
        assertEquals(OnboardingRowRecord.RowStatus.WARNING, warningRow.getStatus());
        assertTrue(warningRow.getValidationErrors().get(0).contains("DUPLICATE_OWNER_SAME_FLAT"));
    }

    @Test
    @DisplayName("Should detect invalid phone numbers, malformed flats, and produce downloadable error CSV")
    void testFatalErrorsAndErrorReportGeneration() {
        String csvContent = "Tower/Flat,Resident_Name,Phone,Occupancy_Type,Vehicle_Number,Opening_Balance\n" +
                "InvalidFlatSyntax,Ananya Rao,12345,OWNER,,0\n" + // Malformed flat & phone
                "A-101,,9876543210,OWNER,,0\n";                    // Missing name

        InputStream stream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        IngestionValidationReportDto report = service.parseAndValidateCsv(communityId, "dirty_errors.csv", stream, true);

        assertNotNull(report);
        assertEquals(2, report.getTotalRows());
        assertEquals(2, report.getFatalErrorRowCount());
        assertFalse(report.isReadyForCommit());

        String errorCsv = service.generateErrorReportCsv(report.getJobId());
        assertNotNull(errorCsv);
        assertTrue(errorCsv.contains("MALFORMED_FLAT_IDENTIFIER"));
        assertTrue(errorCsv.contains("INVALID_PHONE_NUMBER"));
        assertTrue(errorCsv.contains("MISSING_RESIDENT_NAME"));
    }

    @Test
    @DisplayName("Should atomically reject commit and roll back when fatal errors exist")
    void testAtomicRollbackOnFatalErrors() {
        String csvContent = "Tower/Flat,Resident_Name,Phone,Occupancy_Type,Vehicle_Number,Opening_Balance\n" +
                "PenthouseWithoutTower,Unknown,00000,OWNER,,INVALID\n";

        InputStream stream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        IngestionValidationReportDto report = service.parseAndValidateCsv(communityId, "bad.csv", stream, false);

        assertThrows(IllegalStateException.class, () -> service.commitJob(report.getJobId()));

        BulkOnboardingJobDto job = service.getJobStatus(report.getJobId());
        assertEquals(BulkOnboardingJobDto.Status.ROLLED_BACK, job.getStatus());
    }

    @Test
    @DisplayName("Should commit successfully when all rows are valid")
    void testSuccessfulCommit() {
        String csvContent = "Tower/Flat,Resident_Name,Phone,Occupancy_Type,Vehicle_Number,Opening_Balance\n" +
                "Tower C - 501,Vikram Singh,+91 9988776655,OWNER,KA-05-XY-1234,5000 CR\n";

        InputStream stream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        IngestionValidationReportDto report = service.parseAndValidateCsv(communityId, "clean.csv", stream, false);

        BulkOnboardingJobDto committed = service.commitJob(report.getJobId());
        assertNotNull(committed);
        assertEquals(BulkOnboardingJobDto.Status.COMMITTED, committed.getStatus());
        assertNotNull(committed.getCompletedAt());
    }
}
