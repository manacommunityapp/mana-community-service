package com.manacommunity.api.noticeboard.service;

import com.manacommunity.api.event.AnnouncementCreatedEvent;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.noticeboard.dto.NoticeRequest;
import com.manacommunity.api.noticeboard.dto.NoticeResponse;
import com.manacommunity.api.noticeboard.dto.NoticeStatsResponse;
import com.manacommunity.api.noticeboard.engine.NoticeEngine;
import com.manacommunity.api.noticeboard.entity.Notice;
import com.manacommunity.api.noticeboard.entity.NoticeReadReceipt;
import com.manacommunity.api.noticeboard.repository.NoticeReadReceiptRepository;
import com.manacommunity.api.noticeboard.repository.NoticeRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeService {

    private final NoticeRepository repo;
    private final NoticeReadReceiptRepository receiptRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final NoticeEngine noticeEngine;

    @Transactional(readOnly = true)
    public List<NoticeResponse> getActiveNotices(Long communityId, String category, AppUser currentUser) {
        List<Notice> notices;
        if (category != null && !category.isBlank() && !"All".equalsIgnoreCase(category)) {
            Notice.NoticeCategory cat = parseEnum(Notice.NoticeCategory.class, category);
            if (cat != null) {
                notices = repo.findActiveByCommunityAndCategory(communityId, cat, LocalDate.now());
            } else {
                notices = repo.findActiveByCommunity(communityId, LocalDate.now());
            }
        } else {
            notices = repo.findActiveByCommunity(communityId, LocalDate.now());
        }

        return notices.stream()
                .filter(n -> noticeEngine.isUserEligibleForNotice(n, currentUser, null, true))
                .map(n -> toResponse(n, currentUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> getAllNotices(Long communityId, AppUser currentUser) {
        return repo.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream().map(n -> toResponse(n, currentUser)).toList();
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> getMyNotices(Long authorId, AppUser currentUser) {
        return repo.findByAuthorIdOrderByCreatedAtDesc(authorId)
                .stream().map(n -> toResponse(n, currentUser)).toList();
    }

    @Transactional
    public NoticeResponse getById(Long id, AppUser currentUser) {
        Notice notice = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice", id));
        assertSameCommunity(notice.getCommunity(), currentUser);

        // Record read receipt
        recordReadReceipt(notice, currentUser);

        return toResponse(notice, currentUser);
    }

    @Transactional
    public NoticeResponse create(NoticeRequest req, AppUser author, Community community) {
        Notice.NoticeCategory category = parseEnumOrDefault(Notice.NoticeCategory.class, req.getCategory(), Notice.NoticeCategory.GENERAL);
        Notice.NoticePriority priority = parseEnumOrDefault(Notice.NoticePriority.class, req.getPriority(), Notice.NoticePriority.NORMAL);
        Notice.TargetAudience audience = parseEnumOrDefault(Notice.TargetAudience.class, req.getTargetAudience(), Notice.TargetAudience.ALL);

        Notice notice = Notice.builder()
                .title(HtmlSanitizer.sanitizePlainText(req.getTitle()))
                .body(HtmlSanitizer.sanitizeRichText(req.getBody()))
                .category(category)
                .priority(priority)
                .targetAudience(audience)
                .targetBlock(req.getTargetBlock())
                .pinned(req.isPinned())
                .requiresAcknowledgement(req.isRequiresAcknowledgement())
                .attachments(req.getAttachments())
                .status(Notice.NoticeStatus.PUBLISHED)
                .author(author)
                .community(community)
                .build();

        if (req.getExpiresOn() != null && !req.getExpiresOn().isBlank()) {
            notice.setExpiresOn(LocalDate.parse(req.getExpiresOn()));
        }

        if (req.getScheduledPublishAt() != null && !req.getScheduledPublishAt().isBlank()) {
            notice.setScheduledPublishAt(LocalDateTime.parse(req.getScheduledPublishAt()));
            notice.setStatus(Notice.NoticeStatus.SCHEDULED);
        }

        Notice saved = repo.save(notice);

        try {
            eventPublisher.publishEvent(new AnnouncementCreatedEvent(
                    this,
                    saved.getId(),
                    saved.getTitle(),
                    saved.getBody(),
                    saved.getPriority() != null ? saved.getPriority().name() : "NORMAL",
                    community != null ? community.getId() : null,
                    author != null ? author.getId() : null
            ));
        } catch (Exception e) {
            log.warn("Failed to publish AnnouncementCreatedEvent", e);
        }

        return toResponse(saved, author);
    }

    @Transactional
    public NoticeResponse acknowledgeNotice(Long noticeId, AppUser currentUser) {
        Notice notice = repo.findById(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("Notice", noticeId));
        assertSameCommunity(notice.getCommunity(), currentUser);

        NoticeReadReceipt receipt = receiptRepo.findByNoticeIdAndUserId(noticeId, currentUser.getId())
                .orElseGet(() -> NoticeReadReceipt.builder()
                        .notice(notice)
                        .user(currentUser)
                        .readAt(LocalDateTime.now())
                        .build());

        receipt.setAcknowledged(true);
        receipt.setAcknowledgedAt(LocalDateTime.now());
        receiptRepo.save(receipt);

        return toResponse(notice, currentUser);
    }

    @Transactional(readOnly = true)
    public NoticeStatsResponse getNoticeStats(Long noticeId, AppUser currentUser) {
        Notice notice = repo.findById(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("Notice", noticeId));
        assertSameCommunity(notice.getCommunity(), currentUser);

        long reads = receiptRepo.countReadReceipts(noticeId);
        long acks = receiptRepo.countAcknowledgements(noticeId);

        return noticeEngine.computeStats(notice, 100L, reads, acks);
    }

    @Transactional
    public NoticeResponse update(Long id, NoticeRequest req, Long authorId, AppUser currentUser) {
        Notice notice = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice", id));
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
        if (req.getTargetAudience() != null) {
            Notice.TargetAudience aud = parseEnum(Notice.TargetAudience.class, req.getTargetAudience());
            if (aud != null) notice.setTargetAudience(aud);
        }
        if (req.getTargetBlock() != null) {
            notice.setTargetBlock(req.getTargetBlock());
        }
        notice.setPinned(req.isPinned());
        notice.setRequiresAcknowledgement(req.isRequiresAcknowledgement());
        notice.setAttachments(req.getAttachments());
        if (req.getExpiresOn() != null && !req.getExpiresOn().isBlank()) {
            notice.setExpiresOn(LocalDate.parse(req.getExpiresOn()));
        }

        return toResponse(repo.save(notice), currentUser);
    }

    @Transactional
    public void delete(Long id, AppUser currentUser) {
        Notice notice = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice", id));
        assertSameCommunity(notice.getCommunity(), currentUser);
        repo.deleteById(id);
    }

    @Transactional
    public NoticeResponse togglePin(Long id, AppUser currentUser) {
        Notice notice = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice", id));
        assertSameCommunity(notice.getCommunity(), currentUser);
        notice.setPinned(!notice.isPinned());
        return toResponse(repo.save(notice), currentUser);
    }

    private void recordReadReceipt(Notice notice, AppUser user) {
        if (receiptRepo.findByNoticeIdAndUserId(notice.getId(), user.getId()).isEmpty()) {
            NoticeReadReceipt receipt = NoticeReadReceipt.builder()
                    .notice(notice)
                    .user(user)
                    .readAt(LocalDateTime.now())
                    .build();
            receiptRepo.save(receipt);
        }
    }

    private void assertSameCommunity(Community noticeCommunity, AppUser user) {
        Long userCommunityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        Long noticeCommunityId = noticeCommunity != null ? noticeCommunity.getId() : null;
        if (noticeCommunityId == null || !noticeCommunityId.equals(userCommunityId)) {
            throw new AccessDeniedException("Access denied: resource belongs to a different community.");
        }
    }

    private NoticeResponse toResponse(Notice n, AppUser currentUser) {
        boolean isRead = false;
        boolean isAck = false;
        if (currentUser != null) {
            Optional<NoticeReadReceipt> receipt = receiptRepo.findByNoticeIdAndUserId(n.getId(), currentUser.getId());
            isRead = receipt.isPresent();
            isAck = receipt.map(NoticeReadReceipt::isAcknowledged).orElse(false);
        }

        return NoticeResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .body(n.getBody())
                .category(n.getCategory().name())
                .priority(n.getPriority().name())
                .targetAudience(n.getTargetAudience() != null ? n.getTargetAudience().name() : "ALL")
                .targetBlock(n.getTargetBlock())
                .pinned(n.isPinned())
                .requiresAcknowledgement(n.isRequiresAcknowledgement())
                .isReadByCurrentUser(isRead)
                .isAcknowledgedByCurrentUser(isAck)
                .attachments(n.getAttachments())
                .status(n.getStatus() != null ? n.getStatus().name() : "PUBLISHED")
                .expiresOn(n.getExpiresOn() != null ? n.getExpiresOn().toString() : null)
                .scheduledPublishAt(formatDt(n.getScheduledPublishAt()))
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
