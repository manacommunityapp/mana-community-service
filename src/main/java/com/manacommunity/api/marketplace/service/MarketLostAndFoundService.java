package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.marketplace.dto.MarketLostAndFoundRequest;
import com.manacommunity.api.marketplace.dto.MarketLostAndFoundResponse;
import com.manacommunity.api.marketplace.entity.MarketLostAndFound;
import com.manacommunity.api.marketplace.repository.MarketLostAndFoundRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketLostAndFoundService {

    private final MarketLostAndFoundRepository lostAndFoundRepository;

    public Page<MarketLostAndFoundResponse> getCommunityPosts(Long communityId, MarketLostAndFound.PostType type, Pageable pageable) {
        if (type != null) {
            return lostAndFoundRepository.findByCommunityIdAndTypeAndStatus(communityId, type, MarketLostAndFound.LostFoundStatus.OPEN, pageable)
                    .map(this::toResponse);
        }
        return lostAndFoundRepository.findByCommunityIdAndStatus(communityId, MarketLostAndFound.LostFoundStatus.OPEN, pageable)
                .map(this::toResponse);
    }

    public List<MarketLostAndFoundResponse> getMyPosts(Long reporterId) {
        return lostAndFoundRepository.findByReporterIdOrderByCreatedAtDesc(reporterId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public MarketLostAndFoundResponse getById(Long id) {
        return lostAndFoundRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Lost & Found post not found with id: " + id));
    }

    @Transactional
    public MarketLostAndFoundResponse create(MarketLostAndFoundRequest req, AppUser reporter, Community community) {
        MarketLostAndFound post = MarketLostAndFound.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .type(req.getType())
                .category(req.getCategory())
                .imageUrl(req.getImageUrl())
                .location(req.getLocation())
                .dateOccurred(req.getDateOccurred())
                .reporter(reporter)
                .community(community)
                .status(MarketLostAndFound.LostFoundStatus.OPEN)
                .build();

        return toResponse(lostAndFoundRepository.save(post));
    }

    @Transactional
    public MarketLostAndFoundResponse claimItem(Long id, AppUser user) {
        MarketLostAndFound post = lostAndFoundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        if (post.getStatus() != MarketLostAndFound.LostFoundStatus.OPEN) {
            throw new InvalidInputException("Item is no longer open for claims");
        }

        post.setStatus(MarketLostAndFound.LostFoundStatus.CLAIMED);
        post.setClaimedBy(user);

        return toResponse(lostAndFoundRepository.save(post));
    }

    @Transactional
    public MarketLostAndFoundResponse resolvePost(Long id) {
        MarketLostAndFound post = lostAndFoundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        post.setStatus(MarketLostAndFound.LostFoundStatus.RESOLVED);
        return toResponse(lostAndFoundRepository.save(post));
    }

    private MarketLostAndFoundResponse toResponse(MarketLostAndFound p) {
        return MarketLostAndFoundResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .type(p.getType())
                .category(p.getCategory())
                .imageUrl(p.getImageUrl())
                .location(p.getLocation())
                .dateOccurred(p.getDateOccurred())
                .status(p.getStatus())
                .reporterName(p.getReporter().getFullName() != null ? p.getReporter().getFullName() : p.getReporter().getUsername())
                .reporterId(p.getReporter().getId())
                .communityId(p.getCommunity() != null ? p.getCommunity().getId() : null)
                .claimedByName(p.getClaimedBy() != null ? p.getClaimedBy().getFullName() : null)
                .claimedById(p.getClaimedBy() != null ? p.getClaimedBy().getId() : null)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
