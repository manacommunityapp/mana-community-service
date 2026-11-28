package com.manacommunity.api.onboarding.ingestion;

import com.manacommunity.api.onboarding.ingestion.model.BulkOnboardingJobDto;
import com.manacommunity.api.onboarding.ingestion.model.IngestionValidationReportDto;
import com.manacommunity.api.onboarding.ingestion.service.BulkOnboardingIngestionServiceImpl;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BulkOnboardingStressTest {

    @Test
    void processes5000RowSocietyCsvWithMalformedPhonesDuplicateOwnersAndUnicodeWithinSla() {
        BulkOnboardingIngestionServiceImpl service = new BulkOnboardingIngestionServiceImpl();

        StringBuilder sb = new StringBuilder(5000 * 64);
        sb.append("Tower/Flat,Resident_Name,Phone,Occupancy_Type,Vehicle_Number,Opening_Balance\n");
        for (int i = 1; i <= 5000; i++) {
            int towerIdx = (i % 5);
            char tower = (char) ('A' + towerIdx);
            int flatNum = 100 + (i / 5);
            if (i % 100 == 0) {
                // 50 fatal rows (bad phone)
                sb.append(String.format("Tower %c - %d,Bad Phone User,123,OWNER,,0\n", tower, flatNum));
            } else if (i % 50 == 1 && i > 1) {
                // Duplicate owner on previous row's flat
                sb.append(String.format("Tower %c - %d,Duplicate Owner %d,98765%05d,OWNER,,0\n", tower, flatNum, i, i));
            } else {
                // Valid UTF-8 & formatted phone row
                sb.append(String.format("Tower %c - %d,Surya Rao %d,+91 98765-%05d,OWNER,KA-01-AB-%04d,₹ 100 CR\n",
                        tower, flatNum, i, i, i % 10000));
            }
        }

        long startMs = System.currentTimeMillis();
        IngestionValidationReportDto report = service.parseAndValidateCsv(
                4002L,
                "5000_residents_upgrade.csv",
                new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)),
                true
        );
        long elapsedMs = System.currentTimeMillis() - startMs;

        assertEquals(5000, report.getTotalRows());
        assertEquals(50, report.getFatalErrorRowCount());
        assertFalse(report.isReadyForCommit());
        assertTrue(elapsedMs < 5000, "5,000-row dry-run parsing must finish in < 5s (took " + elapsedMs + "ms)");
        assertEquals(BulkOnboardingJobDto.Status.DRY_RUN_COMPLETED, service.getJobStatus(report.getJobId()).getStatus());
    }
}
