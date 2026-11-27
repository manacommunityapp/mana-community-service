package com.manacommunity.api.onboarding.ingestion.service;

import com.manacommunity.api.onboarding.ingestion.model.BulkOnboardingJobDto;
import com.manacommunity.api.onboarding.ingestion.model.IngestionValidationReportDto;
import com.manacommunity.api.onboarding.ingestion.model.OnboardingRowRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BulkOnboardingIngestionServiceImpl implements BulkOnboardingIngestionService {

    private final AtomicLong jobIdGenerator = new AtomicLong(5000);
    private final Map<Long, BulkOnboardingJobDto> jobRegistry = new ConcurrentHashMap<>();
    private final Map<Long, List<OnboardingRowRecord>> stagedRecords = new ConcurrentHashMap<>();

    private static final Pattern FLAT_PATTERN_1 = Pattern.compile("^(?:Tower|Block|Wing)?\\s*([A-Za-z]+)[-\\s,]+(?:Flat\\s*)?(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLAT_PATTERN_2 = Pattern.compile("^([A-Za-z]+)(\\d+)$");

    @Override
    public IngestionValidationReportDto parseAndValidateCsv(Long communityId, String fileName, InputStream inputStream, boolean dryRun) {
        long jobId = jobIdGenerator.incrementAndGet();
        List<OnboardingRowRecord> rows = new ArrayList<>();
        Map<String, Long> errorsByColumn = new HashMap<>();
        Set<String> ownerFlatsSeen = new HashSet<>();
        BigDecimal totalLedgerSum = BigDecimal.ZERO;

        int cleanCount = 0;
        int warningCount = 0;
        int errorCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int rowIdx = 0;

            while ((line = reader.readLine()) != null) {
                rowIdx++;
                if (rowIdx == 1 && (line.toLowerCase().contains("flat") || line.toLowerCase().contains("phone"))) {
                    // Skip CSV header
                    continue;
                }
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] tokens = line.split(",", -1);
                OnboardingRowRecord record = parseRow(rowIdx, tokens);

                // Check duplicate owner for same flat
                if (record.getNormalizedTower() != null && record.getNormalizedFlat() != null) {
                    String flatKey = record.getNormalizedTower() + ":" + record.getNormalizedFlat();
                    if ("OWNER".equalsIgnoreCase(record.getOccupancyType())) {
                        if (ownerFlatsSeen.contains(flatKey)) {
                            record.setStatus(OnboardingRowRecord.RowStatus.WARNING);
                            record.getValidationErrors().add("DUPLICATE_OWNER_SAME_FLAT: Flat " + flatKey + " already has a registered primary owner.");
                            record.setRemediationSuggestion("Change occupancy to TENANT or FAMILY_MEMBER, or mark joint ownership.");
                            warningCount++;
                        } else {
                            ownerFlatsSeen.add(flatKey);
                        }
                    }
                }

                if (record.getStatus() == OnboardingRowRecord.RowStatus.ERROR) {
                    errorCount++;
                    for (String err : record.getValidationErrors()) {
                        String col = err.split(":")[0].trim();
                        errorsByColumn.put(col, errorsByColumn.getOrDefault(col, 0L) + 1);
                    }
                } else if (record.getStatus() == OnboardingRowRecord.RowStatus.WARNING) {
                    // Already counted if duplicate owner, else check
                } else {
                    cleanCount++;
                }

                if (record.getParsedOpeningBalance() != null) {
                    totalLedgerSum = totalLedgerSum.add(record.getParsedOpeningBalance());
                }

                rows.add(record);
            }
        } catch (Exception e) {
            log.error("Failed to parse onboarding CSV", e);
            throw new RuntimeException("Malformed CSV stream: " + e.getMessage(), e);
        }

        stagedRecords.put(jobId, rows);

        BulkOnboardingJobDto.Status initialStatus = dryRun
                ? BulkOnboardingJobDto.Status.DRY_RUN_COMPLETED
                : BulkOnboardingJobDto.Status.PARSED;

        BulkOnboardingJobDto job = BulkOnboardingJobDto.builder()
                .jobId(jobId)
                .communityId(communityId)
                .fileName(fileName)
                .status(initialStatus)
                .totalRows(rows.size())
                .validRows(cleanCount)
                .warningRows(warningCount)
                .errorRows(errorCount)
                .createdAt(LocalDateTime.now())
                .build();

        jobRegistry.put(jobId, job);

        List<OnboardingRowRecord> sampleErrors = rows.stream()
                .filter(r -> r.getStatus() != OnboardingRowRecord.RowStatus.VALID)
                .limit(20)
                .collect(Collectors.toList());

        boolean readyForCommit = errorCount == 0;

        return IngestionValidationReportDto.builder()
                .jobId(jobId)
                .communityId(communityId)
                .totalRows(rows.size())
                .cleanRowCount(cleanCount)
                .warningRowCount(warningCount)
                .fatalErrorRowCount(errorCount)
                .readyForCommit(readyForCommit)
                .openingLedgerSum(totalLedgerSum)
                .errorsByColumn(errorsByColumn)
                .sampleErrors(sampleErrors)
                .build();
    }

    @Override
    public BulkOnboardingJobDto commitJob(Long jobId) {
        BulkOnboardingJobDto job = jobRegistry.get(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        if (job.getErrorRows() > 0) {
            job.setStatus(BulkOnboardingJobDto.Status.ROLLED_BACK);
            job.setCompletedAt(LocalDateTime.now());
            throw new IllegalStateException("Cannot commit job with " + job.getErrorRows() + " fatal row errors. Resolve errors first.");
        }

        // Simulate atomic commit of staged records into production DB
        List<OnboardingRowRecord> records = stagedRecords.get(jobId);
        if (records == null || records.isEmpty()) {
            throw new IllegalStateException("No staged records found for job: " + jobId);
        }

        job.setStatus(BulkOnboardingJobDto.Status.COMMITTED);
        job.setCompletedAt(LocalDateTime.now());
        log.info("Successfully committed {} rows for community {} in job {}", records.size(), job.getCommunityId(), jobId);

        return job;
    }

    @Override
    public BulkOnboardingJobDto rollbackJob(Long jobId) {
        BulkOnboardingJobDto job = jobRegistry.get(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        stagedRecords.remove(jobId);
        job.setStatus(BulkOnboardingJobDto.Status.ROLLED_BACK);
        job.setCompletedAt(LocalDateTime.now());
        log.info("Rolled back job {} for community {}", jobId, job.getCommunityId());
        return job;
    }

    @Override
    public String generateErrorReportCsv(Long jobId) {
        List<OnboardingRowRecord> records = stagedRecords.get(jobId);
        if (records == null) {
            return "Row,Raw_Flat,Raw_Phone,Error_Code,Suggested_Fix\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Row,Raw_Flat,Raw_Phone,Status,Error_Code,Suggested_Fix\n");

        for (OnboardingRowRecord r : records) {
            if (r.getStatus() != OnboardingRowRecord.RowStatus.VALID) {
                String errors = String.join(" | ", r.getValidationErrors()).replace(",", ";");
                String suggestion = r.getRemediationSuggestion() != null ? r.getRemediationSuggestion().replace(",", ";") : "N/A";
                sb.append(r.getRowNumber()).append(",")
                        .append(sanitizeCsv(r.getRawFlatIdentifier())).append(",")
                        .append(sanitizeCsv(r.getRawPhone())).append(",")
                        .append(r.getStatus()).append(",")
                        .append(errors).append(",")
                        .append(suggestion).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public BulkOnboardingJobDto getJobStatus(Long jobId) {
        return jobRegistry.get(jobId);
    }

    private OnboardingRowRecord parseRow(int rowIdx, String[] tokens) {
        String rawFlat = tokens.length > 0 ? tokens[0].trim() : "";
        String residentName = tokens.length > 1 ? tokens[1].trim() : "";
        String rawPhone = tokens.length > 2 ? tokens[2].trim() : "";
        String occupancy = tokens.length > 3 ? tokens[3].trim().toUpperCase() : "OWNER";
        String vehicleNumber = tokens.length > 4 ? tokens[4].trim() : "";
        String rawBalance = tokens.length > 5 ? tokens[5].trim() : "0";

        List<String> errors = new ArrayList<>();
        String remediation = null;
        OnboardingRowRecord.RowStatus status = OnboardingRowRecord.RowStatus.VALID;

        // 1. Normalize Flat / Tower
        String normTower = null;
        String normFlat = null;
        Matcher m1 = FLAT_PATTERN_1.matcher(rawFlat);
        Matcher m2 = FLAT_PATTERN_2.matcher(rawFlat);

        if (m1.matches()) {
            normTower = "TOWER_" + m1.group(1).toUpperCase();
            normFlat = m1.group(2);
        } else if (m2.matches()) {
            normTower = "TOWER_" + m2.group(1).toUpperCase();
            normFlat = m2.group(2);
        } else {
            status = OnboardingRowRecord.RowStatus.ERROR;
            errors.add("MALFORMED_FLAT_IDENTIFIER: Unable to parse tower and flat from '" + rawFlat + "'");
            remediation = "Provide format like 'Tower A - 101' or 'A-101'";
        }

        // 2. Normalize Phone Number
        String normPhone = normalizePhoneNumber(rawPhone);
        if (normPhone == null) {
            status = OnboardingRowRecord.RowStatus.ERROR;
            errors.add("INVALID_PHONE_NUMBER: Phone '" + rawPhone + "' is not a valid 10-digit mobile number");
            remediation = remediation == null ? "Provide valid 10-digit mobile number with or without +91" : remediation;
        }

        // 3. Parse Opening Balance
        BigDecimal parsedBalance = BigDecimal.ZERO;
        try {
            parsedBalance = parseOpeningBalance(rawBalance);
        } catch (Exception e) {
            status = OnboardingRowRecord.RowStatus.ERROR;
            errors.add("INVALID_OPENING_BALANCE: Cannot parse balance '" + rawBalance + "'");
            remediation = remediation == null ? "Specify numeric balance or '12000 CR' / '4500 DR'" : remediation;
        }

        // 4. Validate Resident Name
        if (residentName.isEmpty() || residentName.length() < 2) {
            status = OnboardingRowRecord.RowStatus.ERROR;
            errors.add("MISSING_RESIDENT_NAME: Resident name cannot be blank");
        }

        return OnboardingRowRecord.builder()
                .rowNumber(rowIdx)
                .rawFlatIdentifier(rawFlat)
                .normalizedTower(normTower)
                .normalizedFlat(normFlat)
                .residentName(residentName)
                .rawPhone(rawPhone)
                .normalizedPhone(normPhone)
                .occupancyType(occupancy)
                .vehicleNumber(vehicleNumber)
                .rawOpeningBalance(rawBalance)
                .parsedOpeningBalance(parsedBalance)
                .status(status)
                .validationErrors(errors)
                .remediationSuggestion(remediation)
                .build();
    }

    private String normalizePhoneNumber(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String cleaned = raw.replaceAll("[^0-9+]", "");
        if (cleaned.startsWith("+91")) {
            cleaned = cleaned.substring(3);
        } else if (cleaned.startsWith("91") && cleaned.length() == 12) {
            cleaned = cleaned.substring(2);
        } else if (cleaned.startsWith("0") && cleaned.length() == 11) {
            cleaned = cleaned.substring(1);
        }

        if (cleaned.length() == 10 && cleaned.matches("^[6-9]\\d{9}$")) {
            return "+91" + cleaned;
        }
        return null;
    }

    private BigDecimal parseOpeningBalance(String raw) {
        if (raw == null || raw.trim().isEmpty() || "0".equals(raw.trim())) {
            return BigDecimal.ZERO;
        }
        String clean = raw.replaceAll("[₹,\\s]", "").toUpperCase();
        boolean isDebit = clean.endsWith("DR") || clean.startsWith("-");
        boolean isCredit = clean.endsWith("CR") || clean.startsWith("+");

        clean = clean.replace("CR", "").replace("DR", "").replace("+", "").replace("-", "");
        BigDecimal val = new BigDecimal(clean);
        return isDebit ? val.negate() : val;
    }

    private String sanitizeCsv(String val) {
        return val == null ? "" : val.replace(",", " ").trim();
    }
}
