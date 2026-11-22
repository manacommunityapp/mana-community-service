package com.manacommunity.api.storage;

import com.manacommunity.api.media.repository.MediaRepository;
import com.manacommunity.api.storage.entity.StoredFile;
import com.manacommunity.api.storage.repository.StoredFileRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileStorageController {

    private final FileStorageService storageService;
    private final StoredFileRepository storedFileRepo;
    private final LoggedInUserService loggedInUserService;
    private final AppUserRepository appUserRepository;
    private final PresignedUrlRefreshService refreshService;
    private final MediaRepository mediaRepository;

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
     * only the uploader, a user of the file's community, or a SUPER_ADMIN may
     * read the bytes. The owning community is {@code stored_file.community_id}
     * (added and back-filled in V506), falling back to a lookup through
     * {@code uploaded_by} where that column is still NULL.
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
     *
     * <p>SUPER_ADMIN sees everything. Otherwise:
     * <ol>
     *   <li>the uploader is always entitled to their own file;</li>
     *   <li>if {@code stored_file.community_id} is set (V506, back-filled from the uploader)
     *       it is authoritative — one indexed column read instead of a per-request
     *       {@code app_user} lookup;</li>
     *   <li>if it is NULL the previous behaviour is kept exactly: derive the community from
     *       {@code uploaded_by} through {@code app_user}, so nothing that works today breaks
     *       on a row the V506 back-fill could not attribute (e.g. a file uploaded after V506
     *       by a user whose community was unknown at the time).</li>
     * </ol>
     * A row with neither a community nor an uploader carries no ownership information at all
     * and stays SUPER_ADMIN-only.
     */
    private boolean canAccess(StoredFile file, UserPrincipal principal) {
        LoggedInUserService.ResolvedUser ctx = loggedInUserService.resolveContext(principal);
        if (ctx.superAdmin()) return true;

        Long uploadedBy = file.getUploadedBy();
        if (uploadedBy != null && uploadedBy.equals(principal.getId())) return true;

        Long callerCommunityId = ctx.communityId();
        if (callerCommunityId == null) return false;

        Long fileCommunityId = file.getCommunityId();
        if (fileCommunityId != null) {
            return callerCommunityId.equals(fileCommunityId);
        }

        // Legacy row the back-fill could not attribute: fall back to the uploader lookup.
        if (uploadedBy == null) return false;
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
     * Requires authentication. For the per-user key space
     * ({@code users/{userId}/...}) the caller must be that user, a member of that user's
     * community, or a SUPER_ADMIN; any other key is scoped against the owning
     * {@code media_objects} / {@code stored_file} row when one can be found for it.
     * See {@link #canAccessKey}.
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
        if (key.isBlank() || key.contains("..") || key.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        // Sensitive paths require authenticated access
        if (key.startsWith("kyc/") || key.startsWith("id_proofs/") || key.startsWith("documents/private/")) {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            boolean isAdmin = principal.getAuthorities() != null && principal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().contains("ADMIN") || a.getAuthority().contains("SECURITY"));
            if (!isAdmin && key.startsWith("kyc/")) {
                String userSegment = "user-" + principal.getId();
                String directUserSegment = "/" + principal.getId() + "/";
                if (!key.contains(userSegment) && !key.contains(directUserSegment)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
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
     * <p>Resolution order, most specific first:
     * <ol>
     *   <li>SUPER_ADMIN: always allowed.</li>
     *   <li>Keys in the per-user namespace ({@code users/{userId}/...}): resolved to their
     *       owner and checked against the caller, as before.</li>
     *   <li>Any other key is looked up against {@code media_objects} by all four of its S3
     *       keys (original / thumbnail / compressed / medium). {@code community_id} is NOT
     *       NULL there, so a hit gives a hard tenant check for key namespaces such as
     *       {@code events/...} that carry no owner in the key itself.</li>
     *   <li>Failing that, a key that names a Postgres-backed {@code stored_file} row (by id or
     *       by stored filename — what {@code PostgresFileStorageService#getStream} accepts) is
     *       checked with the same rules as {@code GET /api/files/{id}}, so the wildcard route
     *       cannot be used to side-step the id route's tenancy check.</li>
     *   <li>A key matching no row stays authenticated-only and is logged. It cannot be scoped
     *       from the key alone, and denying it would break objects whose keys were never
     *       persisted against an owning entity (pre-media-service uploads, the
     *       {@code events/...} paths written by {@code /upload/event-payment}).</li>
     * </ol>
     */
    private boolean canAccessKey(String key, UserPrincipal principal) {
        LoggedInUserService.ResolvedUser ctx = loggedInUserService.resolveContext(principal);
        if (ctx.superAdmin()) return true;

        Long ownerId = userIdFromKey(key);
        if (ownerId != null) {
            if (ownerId.equals(principal.getId())) return true;
            Long callerCommunity = ctx.communityId();
            return callerCommunity != null && callerCommunity.equals(communityIdOfUser(ownerId));
        }

        Long callerCommunityId = ctx.communityId();

        Set<Long> mediaOwners = mediaCommunitiesForKey(key);
        if (!mediaOwners.isEmpty()) {
            boolean allowed = callerCommunityId != null && mediaOwners.contains(callerCommunityId);
            if (!allowed) {
                log.warn("Denying media key {} to userId={} (community {}) — object belongs to community {}",
                        key, principal.getId(), callerCommunityId, mediaOwners);
            }
            return allowed;
        }

        StoredFile stored = storedFileForKey(key);
        if (stored != null) {
            return canAccess(stored, principal);
        }

        log.warn("Wildcard file key {} matched no media_objects or stored_file row — serving it to "
                        + "authenticated userId={} (community {}) with no tenant check. Persist the key "
                        + "against its owning entity to make it scopable.",
                key, principal.getId(), callerCommunityId);
        return true;
    }

    /** Communities owning the media object that holds {@code key} as any of its S3 keys. */
    private Set<Long> mediaCommunitiesForKey(String key) {
        String normalized = key.startsWith("/") ? key.substring(1) : key;
        List<Long> ids = mediaRepository.findCommunityIdsByAnyKey(normalized);
        Set<Long> owners = new HashSet<>();
        for (Long id : ids) {
            if (id != null) owners.add(id);
        }
        return owners;
    }

    /**
     * The {@code stored_file} row a wildcard key refers to, if any. Mirrors the lookups
     * {@code PostgresFileStorageService#getStream} performs (numeric id, then stored filename).
     */
    private StoredFile storedFileForKey(String key) {
        String normalized = key.startsWith("/") ? key.substring(1) : key;
        if (normalized.matches("^\\d+$")) {
            StoredFile byId = storedFileRepo.findById(Long.parseLong(normalized)).orElse(null);
            if (byId != null) return byId;
        }
        if (normalized.contains("/")) return null;
        return storedFileRepo.findByFilename(normalized).orElse(null);
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
