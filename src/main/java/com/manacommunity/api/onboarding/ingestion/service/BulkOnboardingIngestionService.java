package com.manacommunity.api.onboarding.ingestion.service;

import com.manacommunity.api.onboarding.ingestion.model.BulkOnboardingJobDto;
import com.manacommunity.api.onboarding.ingestion.model.IngestionValidationReportDto;

import java.io.InputStream;

public interface BulkOnboardingIngestionService {

    IngestionValidationReportDto parseAndValidateCsv(Long communityId, String fileName, InputStream inputStream, boolean dryRun);

    BulkOnboardingJobDto commitJob(Long jobId);

    BulkOnboardingJobDto rollbackJob(Long jobId);

    String generateErrorReportCsv(Long jobId);

    BulkOnboardingJobDto getJobStatus(Long jobId);
}
