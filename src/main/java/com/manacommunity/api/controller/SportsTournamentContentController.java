package com.manacommunity.api.controller;

import com.manacommunity.api.model.SportsTournamentAnnouncement;
import com.manacommunity.api.model.SportsTournamentGalleryImage;
import com.manacommunity.api.model.SportsTournamentTimelineEntry;
import com.manacommunity.api.service.SportsTournamentContentService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Manage the custom content that backs a tournament's announcement email:
 * announcements, gallery images and timeline milestones. Mutations are
 * admin-only; the announcement email falls back to defaults when empty.
 */
@RestController
@RequestMapping("/api/tournaments/{tournamentId}")
@RequiredArgsConstructor
public class SportsTournamentContentController {

    private final SportsTournamentContentService contentService;

    private static final String ADMIN = "hasAnyRole('ADMIN','SUPER_ADMIN','SPORTS_ADMIN','COMMUNITY_ADMIN')";

    // ── Request bodies ───────────────────────────────────────────────────────
    public record AnnouncementRequest(@NotBlank String content, String title, String icon, Integer sortOrder) {}
    public record GalleryRequest(String title, String imageUrl, String bgColor, String icon, Integer sortOrder) {}
    public record TimelineRequest(@NotBlank String title, LocalDate entryDate, String dateLabel,
                                  String description, Integer sortOrder) {}

    // ── Announcements ────────────────────────────────────────────────────────
    @GetMapping("/announcements")
    public List<SportsTournamentAnnouncement> listAnnouncements(@PathVariable Long tournamentId) {
        return contentService.listAnnouncements(tournamentId);
    }

    @PostMapping("/announcements")
    @PreAuthorize(ADMIN)
    public ResponseEntity<SportsTournamentAnnouncement> addAnnouncement(
            @PathVariable Long tournamentId, @org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid AnnouncementRequest req) {
        SportsTournamentAnnouncement saved = contentService.addAnnouncement(
                tournamentId, req.title(), req.content(), req.icon(), req.sortOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/announcements/{id}")
    @PreAuthorize(ADMIN)
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long tournamentId, @PathVariable Long id) {
        contentService.deleteAnnouncement(id);
        return ResponseEntity.noContent().build();
    }

    // ── Gallery ──────────────────────────────────────────────────────────────
    @GetMapping("/gallery")
    public List<SportsTournamentGalleryImage> listGallery(@PathVariable Long tournamentId) {
        return contentService.listGallery(tournamentId);
    }

    @PostMapping("/gallery")
    @PreAuthorize(ADMIN)
    public ResponseEntity<SportsTournamentGalleryImage> addGalleryImage(
            @PathVariable Long tournamentId, @org.springframework.web.bind.annotation.RequestBody GalleryRequest req) {
        SportsTournamentGalleryImage saved = contentService.addGalleryImage(
                tournamentId, req.title(), req.imageUrl(), req.bgColor(), req.icon(), req.sortOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/gallery/{id}")
    @PreAuthorize(ADMIN)
    public ResponseEntity<Void> deleteGalleryImage(@PathVariable Long tournamentId, @PathVariable Long id) {
        contentService.deleteGalleryImage(id);
        return ResponseEntity.noContent().build();
    }

    // ── Timeline ─────────────────────────────────────────────────────────────
    @GetMapping("/timeline")
    public List<SportsTournamentTimelineEntry> listTimeline(@PathVariable Long tournamentId) {
        return contentService.listTimeline(tournamentId);
    }

    @PostMapping("/timeline")
    @PreAuthorize(ADMIN)
    public ResponseEntity<SportsTournamentTimelineEntry> addTimelineEntry(
            @PathVariable Long tournamentId, @org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid TimelineRequest req) {
        SportsTournamentTimelineEntry saved = contentService.addTimelineEntry(
                tournamentId, req.entryDate(), req.dateLabel(), req.title(), req.description(), req.sortOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/timeline/{id}")
    @PreAuthorize(ADMIN)
    public ResponseEntity<Void> deleteTimelineEntry(@PathVariable Long tournamentId, @PathVariable Long id) {
        contentService.deleteTimelineEntry(id);
        return ResponseEntity.noContent().build();
    }
}
