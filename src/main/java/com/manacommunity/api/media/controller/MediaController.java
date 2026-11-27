package com.manacommunity.api.media.controller;

import com.manacommunity.api.media.dto.*;
import com.manacommunity.api.media.entity.MediaModule;
import com.manacommunity.api.media.service.MediaService;
import com.manacommunity.api.user.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Centralized REST API for all media uploads and retrievals.
 *
 * Base path: /api/media
 *
 * Upload flows:
 *   POST /api/media/upload                       — direct multipart upload (images, docs)
 *   POST /api/media/presigned-upload             — get presigned S3 PUT URL (large files, videos)
 *   POST /api/media/presigned-upload/confirm/{sessionId} — confirm after direct S3 PUT
 *
 * Read flows:
 *   GET  /api/media/{id}                         — get single media item by UUID
 *   GET  /api/media/module/{module}/{moduleId}   — paginated list for a module entity
 *   GET  /api/media/module/{module}/{moduleId}/context/{subContext} — filter by sub-context
 *   GET  /api/media/module/{module}/{moduleId}/active — all active items (no pagination)
 *
 * Lifecycle:
 *   DELETE /api/media/{id}                       — soft delete
 *   POST   /api/media/{id}/restore               — restore soft-deleted item
 *   POST   /api/media/{id}/approve               — approve pending item
 *   POST   /api/media/{id}/reject                — reject with reason
 */
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Media Management", description = "Centralized file upload and retrieval for all modules")
public class MediaController {

    private final MediaService mediaService;
    private final com.manacommunity.api.user.service.LoggedInUserService loggedInUserService;

    // ── Upload ────────────────────────────────────────────────────────────────

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a file directly through the server to S3")
    public ResponseEntity<MediaResponse> upload(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("request") MediaUploadRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long userId = resolveUserId(principal);
        MediaResponse response = mediaService.uploadMedia(file, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/presigned-upload")
    @Operation(summary = "Generate a presigned S3 PUT URL for direct browser→S3 upload")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(
            @Valid @RequestBody PresignedUploadRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long userId = resolveUserId(principal);
        return ResponseEntity.ok(mediaService.generatePresignedUploadUrl(request, userId));
    }

    @PostMapping("/presigned-upload/confirm/{sessionId}")
    @Operation(summary = "Confirm a completed presigned S3 PUT — creates the media record")
    public ResponseEntity<MediaResponse> confirmPresignedUpload(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long userId = resolveUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediaService.confirmPresignedUpload(sessionId, userId));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get a single media item by its external UUID")
    public ResponseEntity<MediaResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(mediaService.getById(id, communityScope(principal)));
    }

    @GetMapping("/module/{module}/{moduleId}")
    @Operation(summary = "Paginated list of media for a specific module entity")
    public ResponseEntity<Page<MediaResponse>> getByModule(
            @PathVariable MediaModule module,
            @PathVariable String moduleId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(mediaService.getByModule(
                module, moduleId, communityScope(principal),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadedAt"))));
    }

    @GetMapping("/module/{module}/{moduleId}/active")
    @Operation(summary = "All active media for a module entity (no pagination)")
    public ResponseEntity<List<MediaResponse>> getActiveByModule(
            @PathVariable MediaModule module,
            @PathVariable String moduleId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(
                mediaService.getActiveByModule(module, moduleId, communityScope(principal)));
    }

    @GetMapping("/module/{module}/{moduleId}/context/{subContext}")
    @Operation(summary = "Active media filtered by sub-context (gallery, banner, sponsor …)")
    public ResponseEntity<List<MediaResponse>> getByModuleAndSubContext(
            @PathVariable MediaModule module,
            @PathVariable String moduleId,
            @PathVariable String subContext,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(mediaService.getByModuleAndSubContext(
                module, moduleId, communityScope(principal), subContext));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a media item")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        mediaService.softDelete(id, communityScope(principal), resolveUserId(principal));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore a soft-deleted media item")
    public ResponseEntity<MediaResponse> restore(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(
                mediaService.restore(id, communityScope(principal), resolveUserId(principal)));
    }

    // ── Approval workflow ─────────────────────────────────────────────────────

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending media item")
    public ResponseEntity<MediaResponse> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(
                mediaService.approve(id, communityScope(principal), resolveUserId(principal)));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending media item with a reason")
    public ResponseEntity<MediaResponse> reject(
            @PathVariable UUID id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(mediaService.reject(
                id, communityScope(principal), reason, resolveUserId(principal)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Long resolveUserId(UserPrincipal principal) {
        // Previously fell back to -1L, which silently stamped rows with a non-existent
        // uploader and left them without recoverable ownership. Every /api/media route
        // now requires authentication, so a null principal is a misconfiguration.
        if (principal == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        return principal.getId();
    }

    /**
     * Tenant scope for media lookups: {@code null} for a SUPER_ADMIN (platform-wide),
     * otherwise the caller's community id. {@code media_objects.community_id} is
     * NOT NULL, so a non-super-admin without a community can match nothing and is
     * rejected rather than silently falling back to an unscoped query.
     */
    private Long communityScope(UserPrincipal principal) {
        if (principal == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        var ctx = loggedInUserService.resolveContext(principal);
        if (ctx.superAdmin()) return null;
        Long communityId = ctx.communityId();
        if (communityId == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No community associated with this account");
        }
        return communityId;
    }
}
