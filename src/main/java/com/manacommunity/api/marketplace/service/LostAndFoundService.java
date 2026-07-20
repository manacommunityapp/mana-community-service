package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.marketplace.dto.LostAndFoundRequest;
import com.manacommunity.api.marketplace.dto.LostAndFoundResponse;
import com.manacommunity.api.marketplace.entity.LostAndFound;
import com.manacommunity.api.marketplace.repository.LostAndFoundRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LostAndFoundService {

    private final LostAndFoundRepository lostAndFoundRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<LostAndFoundResponse> getCommunityPosts(Long communityId, String type, Pageable pageable) {
        if (type == null || type.isBlank()) {
            return lostAndFoundRepo.findByCommunityIdAndStatusOrderByCreatedAtDesc(
                    communityId, LostAndFound.LostFoundStatus.OPEN, pageable)
                    .map(this::toResponse);
        }
        LostAndFound.PostType postType = LostAndFound.PostType.valueOf(type.toUpperCase());
        return lostAndFoundRepo.findByCommunityIdAndTypeAndStatusOrderByCreatedAtDesc(
                communityId, postType, LostAndFound.LostFoundStatus.OPEN, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<LostAndFoundResponse> getMyPosts(Long reporterId) {
        return lostAndFoundRepo.findByReporterIdOrderByCreatedAtDesc(reporterId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public LostAndFoundResponse create(LostAndFoundRequest req, AppUser reporter, Community community) {
        LostAndFound post = LostAndFound.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .type(LostAndFound.PostType.valueOf(req.getType().toUpperCase()))
                .category(req.getCategory())
                .imageUrl(req.getImageUrl())
                .location(req.getLocation())
                .dateOccurred(req.getDateOccurred())
                .reporter(reporter)
                .community(community)
                .build();

        LostAndFound saved = lostAndFoundRepo.save(post);
        auditService.record(AuditAction.PRODUCT_CREATED, AuditModule.MARKETPLACE,
                "LostAndFound", String.valueOf(saved.getId()));
        return toResponse(saved);
    }

    @Transactional
    public LostAndFoundResponse resolve(Long id, Long reporterId) {
        LostAndFound post = lostAndFoundRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
        if (!post.getReporter().getId().equals(reporterId)) {
            throw new IllegalArgumentException("You can only resolve your own posts");
        }

        post.setStatus(LostAndFound.LostFoundStatus.RESOLVED);
        LostAndFound saved = lostAndFoundRepo.save(post);
        auditService.record(AuditAction.PRODUCT_UPDATED, AuditModule.MARKETPLACE,
                "LostAndFound", String.valueOf(saved.getId()));
        return toResponse(saved);
    }

    @Transactional
    public void close(Long id) {
        LostAndFound post = lostAndFoundRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));

        post.setStatus(LostAndFound.LostFoundStatus.CLOSED);
        lostAndFoundRepo.save(post);
        auditService.record(AuditAction.PRODUCT_DELETED, AuditModule.MARKETPLACE,
                "LostAndFound", String.valueOf(id));
    }

    private LostAndFoundResponse toResponse(LostAndFound lf) {
        AppUser r = lf.getReporter();
        return LostAndFoundResponse.builder()
                .id(lf.getId())
                .title(lf.getTitle())
                .description(lf.getDescription())
                .type(lf.getType() != null ? lf.getType().name() : null)
                .category(lf.getCategory())
                .imageUrl(lf.getImageUrl())
                .location(lf.getLocation())
                .dateOccurred(lf.getDateOccurred())
                .status(lf.getStatus().name())
                .reporter(LostAndFoundResponse.ReporterRef.builder()
                        .id(r.getId())
                        .fullName(r.getFullName())
                        .build())
                .communityId(lf.getCommunity() != null ? lf.getCommunity().getId() : null)
                .createdAt(lf.getCreatedAt())
                .build();
    }
}
