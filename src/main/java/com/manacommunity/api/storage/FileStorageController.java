package com.manacommunity.api.storage;

import com.manacommunity.api.storage.entity.StoredFile;
import com.manacommunity.api.storage.repository.StoredFileRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
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
    private final AppUserRepository appUserRepository;
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
     *
     * Ids are sequential integers, so the caller's tenancy MUST be checked:
     * only the uploader, a user of the uploader's community, or a SUPER_ADMIN
     * may read the bytes. {@code stored_file} has no community column, so the
     * owning community is derived from {@code uploaded_by}.
     */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<byte[]> download(@PathVariable Long id,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        StoredFile file = storedFileRepo.findById(id).orElse(null);
        if (file == null) return ResponseEntity.notFound().build();

        if (!canAccess(file, principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getOriginalName() + "\"")
                // Per-user authorized content: must never be stored in a shared/CDN cache.
                .header(HttpHeaders.CACHE_CONTROL, org.springframework.http.CacheControl.maxAge(1, java.util.concurrent.TimeUnit.DAYS).cachePrivate().getHeaderValue())
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .contentLength(file.getSizeBytes())
                .body(file.getData());
    }

    /**
     * True when {@code principal} is entitled to the given stored file.
     * SUPER_ADMIN sees everything; otherwise the file must have been uploaded
     * by the caller, or by a user belonging to the caller's community. Files
     * with no {@code uploaded_by} (legacy rows) have no ownership information
     * at all and are therefore SUPER_ADMIN-only.
     */
    private boolean canAccess(StoredFile file, UserPrincipal principal) {
        LoggedInUserService.ResolvedUser ctx = loggedInUserService.resolveContext(principal);
        if (ctx.superAdmin()) return true;

        Long uploadedBy = file.getUploadedBy();
        if (uploadedBy == null) return false;
        if (uploadedBy.equals(principal.getId())) return true;

        Long callerCommunityId = ctx.communityId();
        if (callerCommunityId == null) return false;
        return callerCommunityId.equals(communityIdOfUser(uploadedBy));
    }

    /** Community a user belongs to, or {@code null} if unknown. */
    private Long communityIdOfUser(Long userId) {
        if (userId == null) return null;
        return appUserRepository.findById(userId)
                .map(AppUser::getCommunity)
                .map(c -> c.getId())
                .orElse(null);
    }

    /**
     * Stream any S3 or persistent file by wildcard path/key (e.g. /api/files/users/14/gallery/original/xxx.jpg).
     *
     * Requires authentication, and for the per-user key space
     * ({@code users/{userId}/...}) the caller must be that user, a member of
     * that user's community, or a SUPER_ADMIN.
     */
    @GetMapping("/**")
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> streamFile(
            jakarta.servlet.http.HttpServletRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String fullPath = (String) request.getAttribute(org.springframework.web.servlet.HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (fullPath == null || !fullPath.startsWith("/api/files/")) {
            return ResponseEntity.badRequest().build();
        }
        String key = fullPath.substring("/api/files/".length()).trim();
        if (key.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        // Reject traversal attempts outright.
        if (key.contains("..")) {
            return ResponseEntity.badRequest().build();
        }
        if (!canAccessKey(key, principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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
        // Per-user authorized content: must never be stored in a shared/CDN cache.
        headers.setCacheControl(org.springframework.http.CacheControl.maxAge(1, java.util.concurrent.TimeUnit.DAYS).cachePrivate().getHeaderValue());

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

    /**
     * Tenancy check for wildcard S3/persistent keys.
     *
     * Keys in the per-user namespace ({@code users/{userId}/...}) are resolved
     * to their owner and checked against the caller. Other key namespaces
     * (e.g. {@code events/...}) carry no owner in the key itself, so they are
     * allowed to any authenticated caller — the anonymous enumeration hole is
     * closed by SecurityConfig, but true per-tenant scoping of those keys needs
     * the key to be persisted against its owning entity.
     */
    private boolean canAccessKey(String key, UserPrincipal principal) {
        LoggedInUserService.ResolvedUser ctx = loggedInUserService.resolveContext(principal);
        if (ctx.superAdmin()) return true;

        Long ownerId = userIdFromKey(key);
        if (ownerId == null) return true;
        if (ownerId.equals(principal.getId())) return true;

        Long callerCommunityId = ctx.communityId();
        return callerCommunityId != null && callerCommunityId.equals(communityIdOfUser(ownerId));
    }

    /** Extracts {userId} from a {@code users/{userId}/...} key, else null. */
    private Long userIdFromKey(String key) {
        String normalized = key.startsWith("/") ? key.substring(1) : key;
        if (!normalized.startsWith("users/")) return null;
        String rest = normalized.substring("users/".length());
        int slash = rest.indexOf('/');
        String segment = slash >= 0 ? rest.substring(0, slash) : rest;
        try {
            return Long.parseLong(segment);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Delete a file by id (Postgres only; S3 deletion needs a separate key). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        StoredFile file = storedFileRepo.findById(id).orElse(null);
        if (file == null) return ResponseEntity.noContent().build();
        if (!canAccess(file, principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        storageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
