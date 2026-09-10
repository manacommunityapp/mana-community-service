package com.manacommunity.api.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.SportsTournament;
import com.manacommunity.api.model.SportsTournamentAnnouncement;
import com.manacommunity.api.model.SportsTournamentGalleryImage;
import com.manacommunity.api.model.SportsTournamentTimelineEntry;
import com.manacommunity.api.repository.SportsTournamentAnnouncementRepository;
import com.manacommunity.api.repository.SportsTournamentGalleryImageRepository;
import com.manacommunity.api.repository.SportsTournamentRepository;
import com.manacommunity.api.repository.SportsTournamentTimelineEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * CRUD for the custom content shown in a tournament's announcement email:
 * announcements, gallery images and timeline milestones.
 */
@Service
@RequiredArgsConstructor
public class SportsTournamentContentService {

    private final SportsTournamentRepository tournamentRepository;
    private final SportsTournamentAnnouncementRepository announcementRepository;
    private final SportsTournamentGalleryImageRepository galleryRepository;
    private final SportsTournamentTimelineEntryRepository timelineRepository;

    private SportsTournament requireTournament(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsTournament", tournamentId));
    }

    // ── Announcements ────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<SportsTournamentAnnouncement> listAnnouncements(Long tournamentId) {
        return announcementRepository.findByTournamentIdOrderBySortOrderAscIdAsc(tournamentId);
    }

    @Transactional
    public SportsTournamentAnnouncement addAnnouncement(Long tournamentId, String title, String content,
                                                  String icon, Integer sortOrder) {
        SportsTournament tournament = requireTournament(tournamentId);
        return announcementRepository.save(SportsTournamentAnnouncement.builder()
                .tournament(tournament)
                .title(title)
                .content(content)
                .icon(icon)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .build());
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        if (!announcementRepository.existsById(id)) throw new ResourceNotFoundException("SportsTournamentAnnouncement", id);
        announcementRepository.deleteById(id);
    }

    // ── Gallery ──────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<SportsTournamentGalleryImage> listGallery(Long tournamentId) {
        return galleryRepository.findByTournamentIdOrderBySortOrderAscIdAsc(tournamentId);
    }

    @Transactional
    public SportsTournamentGalleryImage addGalleryImage(Long tournamentId, String title, String imageUrl,
                                                  String bgColor, String icon, Integer sortOrder) {
        SportsTournament tournament = requireTournament(tournamentId);
        return galleryRepository.save(SportsTournamentGalleryImage.builder()
                .tournament(tournament)
                .title(title)
                .imageUrl(imageUrl)
                .bgColor(bgColor)
                .icon(icon)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .build());
    }

    @Transactional
    public void deleteGalleryImage(Long id) {
        if (!galleryRepository.existsById(id)) throw new ResourceNotFoundException("SportsTournamentGalleryImage", id);
        galleryRepository.deleteById(id);
    }

    // ── Timeline ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<SportsTournamentTimelineEntry> listTimeline(Long tournamentId) {
        return timelineRepository.findByTournamentIdOrderBySortOrderAscIdAsc(tournamentId);
    }

    @Transactional
    public SportsTournamentTimelineEntry addTimelineEntry(Long tournamentId, LocalDate entryDate, String dateLabel,
                                                    String title, String description, Integer sortOrder) {
        SportsTournament tournament = requireTournament(tournamentId);
        return timelineRepository.save(SportsTournamentTimelineEntry.builder()
                .tournament(tournament)
                .entryDate(entryDate)
                .dateLabel(dateLabel)
                .title(title)
                .description(description)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .build());
    }

    @Transactional
    public void deleteTimelineEntry(Long id) {
        if (!timelineRepository.existsById(id)) throw new ResourceNotFoundException("SportsTournamentTimelineEntry", id);
        timelineRepository.deleteById(id);
    }
}
