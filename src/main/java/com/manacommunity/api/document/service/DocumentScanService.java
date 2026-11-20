package com.manacommunity.api.document.service;

import com.manacommunity.api.document.entity.DocScanStatus;
import com.manacommunity.api.document.entity.DocumentVersion;
import com.manacommunity.api.document.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Async scan stub — marks CLEAN for known-safe MIME types.
 * Replace with real ClamAV/Lambda integration via the scan-result callback endpoint.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentScanService {

    private final DocumentVersionRepository versionRepo;

    @Async
    public void scanAsync(Long versionId) {
        DocumentVersion version = versionRepo.findById(versionId).orElse(null);
        if (version == null) return;
        try {
            String ct = version.getContentType();
            boolean safe = ct != null && (
                    ct.startsWith("application/pdf") ||
                    ct.startsWith("image/") ||
                    ct.equals("application/msword") ||
                    ct.startsWith("application/vnd.openxmlformats")
            );
            version.setScanStatus(safe ? DocScanStatus.CLEAN : DocScanStatus.SCAN_FAILED);
            version.setScanCompletedAt(Instant.now());
            versionRepo.save(version);
            log.info("[DocScan] version {} → {}", versionId, version.getScanStatus());
        } catch (Exception e) {
            log.error("[DocScan] version {} scan failed", versionId, e);
            version.setScanStatus(DocScanStatus.SCAN_FAILED);
            version.setScanCompletedAt(Instant.now());
            versionRepo.save(version);
        }
    }
}
