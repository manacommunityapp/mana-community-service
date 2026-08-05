package com.manacommunity.api.noticeboard.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.noticeboard.dto.NoticeRequest;
import com.manacommunity.api.noticeboard.dto.NoticeResponse;
import com.manacommunity.api.noticeboard.entity.Notice;
import com.manacommunity.api.noticeboard.repository.NoticeRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository repo;

    @Transactional(readOnly = true)
    public List<NoticeResponse> getActiveNotices(Long communityId, String category) {
        if (category != null && !category.isBlank() && !"All".equalsIgnoreCase(category)) {
            Notice.NoticeCategory cat = parseEnum(Notice.NoticeCategory.class, category);
            if (cat != null) {
                return repo.findActiveByCommunityAndCategory(communityId, cat, LocalDate.now())
                        .stream().map(this::toResponse).toList();
            }
        }
        return repo.findActiveByCommunity(communityId, LocalDate.now())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> getAllNotices(Long communityId) {
        return repo.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> getMyNotices(Long authorId) {
        return repo.findByAuthorIdOrderByCreatedAtDesc(authorId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public NoticeResponse getById(Long id, AppUser currentUser) {
        Notice notice = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found: " + id));
        // Enforce tenant boundary — prevent cross-community reads
        assertSameCommunity(notice.getCommunity(), currentUser);
        return toResponse(notice);
    }

    @Transactional
    public NoticeResponse create(NoticeRequest req, AppUser author, Community community) {
        Notice notice = Notice.builder()
                .title(HtmlSanitizer.sanitizePlainText(req.getTitle()))
                .body(HtmlSanitizer.sanitizeRichText(req.getBody()))
                .category(parseEnumOrDefault(Notice.NoticeCategory.class, req.getCategory(), Notice.NoticeCategory.GENERAL))
                .priority(parseEnumOrDefault(Notice.NoticePriority.class, req.getPriority(), Notice.NoticePriority.NORMAL))
                .pinned(req.isPinned())
                .author(author)
                .community(community)
                .build();

        if (req.getExpiresOn() != null && !req.getExpiresOn().isBlank()) {
            notice.setExpiresOn(LocalDate.parse(req.getExpiresOn()));
        }

        return toResponse(repo.save(notice));
    }

    @Transactional
    public NoticeResponse update(Long id, NoticeRequest req, Long authorId) {
        Notice notice = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found: " + id));
        if (!notice.getAuthor().getId().equals(authorId)) {
            throw new IllegalArgumentException("You can only edit your own notices");
        }

        notice.setTitle(HtmlSanitizer.sanitizePlainText(req.getTitle()));
        notice.setBody(HtmlSanitizer.sanitizeRichText(req.getBody()));
        if (req.getCategory() != null) {
            Notice.NoticeCategory cat = parseEnum(Notice.NoticeCategory.class, req.getCategory());
            if (cat != null) notice.setCategory(cat);
        }
        if (req.getPriority() != null) {
            Notice.NoticePriority pri = parseEnum(Notice.NoticePriority.class, req.getPriority());
            if (pri != null) notice.setPriority(pri);
        }
        notice.setPinned(req.isPinned());
        if (req.getExpiresOn() != null && !req.getExpiresOn().isBlank()) {
            notice.setExpiresOn(LocalDate.parse(req.getExpiresOn()));
        }

        return toResponse(repo.save(notice));
    }

    @Transactional
    public void delete(Long id, AppUser currentUser) {
        Notice notice = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found: " + id));
        assertSameCommunity(notice.getCommunity(), currentUser);
        repo.deleteById(id);
    }

    @Transactional
    public NoticeResponse togglePin(Long id, AppUser currentUser) {
        Notice notice = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found: " + id));
        assertSameCommunity(notice.getCommunity(), currentUser);
        notice.setPinned(!notice.isPinned());
        return toResponse(repo.save(notice));
    }

    /**
     * Asserts the notice's community matches the current user's community.
     * Prevents IDOR cross-tenant access (users from Community A acting on Community B notices).
     */
    private void assertSameCommunity(Community noticeCommunity, AppUser user) {
        Long userCommunityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        Long noticeCommunityId = noticeCommunity != null ? noticeCommunity.getId() : null;
        if (noticeCommunityId == null || !noticeCommunityId.equals(userCommunityId)) {
            throw new AccessDeniedException("Access denied: resource belongs to a different community.");
        }
    }

    private NoticeResponse toResponse(Notice n) {
        return NoticeResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .body(n.getBody())
                .category(n.getCategory().name())
                .priority(n.getPriority().name())
                .pinned(n.isPinned())
                .expiresOn(n.getExpiresOn() != null ? n.getExpiresOn().toString() : null)
                .authorId(n.getAuthor().getId())
                .authorName(n.getAuthor().getFullName())
                .communityId(n.getCommunity() != null ? n.getCommunity().getId() : null)
                .createdAt(formatDt(n.getCreatedAt()))
                .updatedAt(formatDt(n.getUpdatedAt()))
                .build();
    }

    private String formatDt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) return null;
        try { return Enum.valueOf(enumClass, value); }
        catch (IllegalArgumentException e) { return null; }
    }

    private <E extends Enum<E>> E parseEnumOrDefault(Class<E> enumClass, String value, E defaultVal) {
        E result = parseEnum(enumClass, value);
        return result != null ? result : defaultVal;
    }
}
