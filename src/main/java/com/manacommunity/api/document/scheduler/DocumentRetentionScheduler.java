package com.manacommunity.api.document.scheduler;

import com.manacommunity.api.document.entity.Document;
import com.manacommunity.api.document.entity.DocumentStatus;
import com.manacommunity.api.document.repository.DocumentRepository;
import com.manacommunity.api.document.repository.DocumentVersionRepository;
import com.manacommunity.api.document.service.DocumentS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentRetentionScheduler {

    private final DocumentRepository documentRepo;
    private final DocumentVersionRepository versionRepo;
    private final DocumentS3Service s3Service;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void enforceRetention() {
        documentRepo.findPastRetention(Instant.now()).forEach(doc -> {
            Instant retentionCutoff = doc.getCreatedAt()
                    .plus(doc.getRetentionDays(), ChronoUnit.DAYS);
            if (Instant.now().isAfter(retentionCutoff)) {
                versionRepo.findByDocumentIdOrderByVersionNumberDesc(doc.getId())
                        .forEach(v -> {
                            try {
                                s3Service.deleteObject(v.getS3Key(), v.getS3Bucket());
                            } catch (Exception e) {
                                log.error("[DocRetention] S3 delete failed for key={}", v.getS3Key(), e);
                            }
                        });
                doc.setStatus(DocumentStatus.DELETED);
                documentRepo.save(doc);
                log.info("[DocRetention] Document {} deleted by retention policy ({}d)",
                        doc.getId(), doc.getRetentionDays());
            }
        });
    }
}
