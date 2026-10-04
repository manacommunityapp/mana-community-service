package com.manacommunity.api.storage;

import com.manacommunity.api.storage.entity.StoredFile;
import com.manacommunity.api.storage.repository.StoredFileRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileStorageController {

    private final FileStorageService storageService;
    private final StoredFileRepository storedFileRepo;
    private final LoggedInUserService loggedInUserService;
    private final PresignedUrlRefreshService refreshService;

    /** Upload a file; returns metadata including the URL to persist. */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoredFileDto> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        StoredFileDto dto = storageService.store(file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Upload an event payment screenshot with hierarchical S3 path:
     * events/{eventId}/{eventName}/{block}/{flatNo}/payment_screenshot_{timestamp}.{ext}
     */
    @PostMapping(value = "/upload/event-payment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoredFileDto> uploadEventPaymentScreenshot(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "eventId", required = false) String eventId,
            @RequestParam(value = "eventName", required = false) String eventName,
            @RequestParam(value = "block", required = false) String block,
            @RequestParam(value = "flatNo", required = false) String flatNo,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;

        String safeEventId = (eventId != null && !eventId.isBlank()) ? sanitizePathSegment(eventId) : "event";
        String safeEventName = (eventName != null && !eventName.isBlank()) ? sanitizePathSegment(eventName) : "general-event";
        String safeBlock = (block != null && !block.isBlank()) ? sanitizePathSegment(block) : "general-block";
        String safeFlatNo = (flatNo != null && !flatNo.isBlank()) ? sanitizePathSegment(flatNo) : "general-flat";

        // S3 Path: events/{eventId}/{eventName}/{block}/{flatNo}
        String customPath = String.format("events/%s/%s/%s/%s", safeEventId, safeEventName, safeBlock, safeFlatNo);

        StoredFileDto dto = storageService.store(file, userId, customPath);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    private String sanitizePathSegment(String input) {
        if (input == null) return "na";
        String clean = input.trim()
                .toLowerCase()
                .replaceAll("[^a-zA-Z0-9_-]+", "-")
                .replaceAll("^-+|-+$", "");
        return clean.isBlank() ? "na" : clean;
    }

    /**
     * Serve a Postgres-stored file by id.
     */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        StoredFile file = storedFileRepo.findById(id).orElse(null);
        if (file == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getOriginalName() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, org.springframework.http.CacheControl.maxAge(1, java.util.concurrent.TimeUnit.DAYS).cachePublic().getHeaderValue())
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .contentLength(file.getSizeBytes())
                .body(file.getData());
    }

    /**
     * Stream any S3 or persistent file by wildcard path/key (e.g. /api/files/users/14/gallery/original/xxx.jpg).
     */
    @GetMapping("/**")
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> streamFile(
            jakarta.servlet.http.HttpServletRequest request) {
        String fullPath = (String) request.getAttribute(org.springframework.web.servlet.HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (fullPath == null || !fullPath.startsWith("/api/files/")) {
            return ResponseEntity.badRequest().build();
        }
        String key = fullPath.substring("/api/files/".length()).trim();
        if (key.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        FileStreamResource resource = storageService.getStream(key);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                resource.getContentType() != null ? resource.getContentType() : "application/octet-stream"));
        if (resource.getContentLength() != null) {
            headers.setContentLength(resource.getContentLength());
        }
        if (resource.getETag() != null) {
            headers.setETag(resource.getETag());
        }
        if (resource.getFilename() != null) {
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"");
        }
        headers.setCacheControl(org.springframework.http.CacheControl.maxAge(1, java.util.concurrent.TimeUnit.DAYS).cachePublic().getHeaderValue());

        return new ResponseEntity<>(resource.getBody(), headers, HttpStatus.OK);
    }

    /**
     * Refresh an expired S3 presigned URL. The frontend calls this when an
     * image returns 403 due to URL expiry.
     */
    @PostMapping("/refresh-url")
    public ResponseEntity<Map<String, String>> refreshPresignedUrl(@RequestBody Map<String, String> body) {
        String expiredUrl = body.get("url");
        if (expiredUrl == null || expiredUrl.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing 'url' field"));
        }
        String freshUrl = refreshService.forceRefresh(expiredUrl);
        return ResponseEntity.ok(Map.of("url", freshUrl));
    }

    /** Delete a file by id (Postgres only; S3 deletion needs a separate key). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        storageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
