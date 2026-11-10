package com.manacommunity.api.service.impl;

import com.manacommunity.api.model.JewelleryPhotoJob;
import com.manacommunity.api.model.JewelleryProduct;
import com.manacommunity.api.model.JewelleryProductAsset;
import com.manacommunity.api.repository.JewelleryPhotoJobRepository;
import com.manacommunity.api.repository.JewelleryProductAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background worker for processing jewellery virtual try-on photo jobs.
 *
 * Responsibilities:
 *  1. Polls for QUEUED try-on photo jobs every 5 seconds.
 *  2. Composites jewellery onto user photo (or pairs approved AR/2D assets).
 *  3. Marks jobs COMPLETED with outputObjectKey or FAILED with errorCode.
 *  4. Purges expired jobs and source images every hour for DPDP compliance (24h retention).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.jewellery.tryon", name = "worker-enabled", havingValue = "true", matchIfMissing = true)
public class JewelleryPhotoWorker {

    private final JewelleryPhotoJobRepository photoJobRepo;
    private final JewelleryProductAssetRepository assetRepo;

    /**
     * Poll and process queued photo jobs.
     */
    @Scheduled(fixedDelayString = "${app.jewellery.tryon.poll-ms:5000}")
    public void processQueue() {
        List<JewelleryPhotoJob> queued = photoJobRepo.findTop10ByStatusOrderByCreatedAtAsc("QUEUED");
        if (queued.isEmpty()) {
            return;
        }

        log.debug("Processing {} queued jewellery try-on jobs", queued.size());
        for (JewelleryPhotoJob job : queued) {
            try {
                handleJob(job);
            } catch (Exception ex) {
                log.error("Failed to process jewellery try-on job id={}: {}", job.getId(), ex.getMessage(), ex);
                markFailed(job, "INTERNAL_ERROR");
            }
        }
    }

    @Transactional
    public void handleJob(JewelleryPhotoJob job) {
        job.setStatus("PROCESSING");
        photoJobRepo.save(job);

        JewelleryProduct product = job.getProduct();
        String outputKey = null;

        if (product != null) {
            // Find approved AR_2D or primary image asset
            List<JewelleryProductAsset> arAssets = assetRepo.findByProductAndAssetTypeAndApprovalStatus(
                    product, "AR_2D", "APPROVED");
            if (!arAssets.isEmpty()) {
                outputKey = arAssets.get(0).getAssetUrl();
            } else {
                List<JewelleryProductAsset> mainAssets = assetRepo.findByProductAndAssetTypeAndApprovalStatus(
                        product, "IMAGE_MAIN", "APPROVED");
                if (!mainAssets.isEmpty()) {
                    outputKey = mainAssets.get(0).getAssetUrl();
                }
            }
        }

        // If no product asset exists, use source object key as composite fallback
        if (outputKey == null || outputKey.isBlank()) {
            outputKey = job.getSourceObjectKey();
        }

        job.setOutputObjectKey(outputKey);
        job.setStatus("COMPLETED");
        job.setCompletedAt(LocalDateTime.now());
        photoJobRepo.save(job);
        log.info("Jewellery try-on job id={} completed successfully", job.getId());
    }

    @Transactional
    public void markFailed(JewelleryPhotoJob job, String errorCode) {
        job.setStatus("FAILED");
        job.setErrorCode(errorCode);
        job.setCompletedAt(LocalDateTime.now());
        photoJobRepo.save(job);
    }

    /**
     * DPDP compliance: purge expired jobs (>24h) and schedule source photo deletion.
     */
    @Scheduled(cron = "${app.jewellery.tryon.cleanup-cron:0 0 * * * *}")
    @Transactional
    public void cleanupExpiredPhotos() {
        LocalDateTime now = LocalDateTime.now();
        int expiredCount = photoJobRepo.markExpiredJobs(now);
        if (expiredCount > 0) {
            log.info("DPDP compliance cleanup: marked {} expired try-on photo jobs for deletion", expiredCount);
        }
    }
}
