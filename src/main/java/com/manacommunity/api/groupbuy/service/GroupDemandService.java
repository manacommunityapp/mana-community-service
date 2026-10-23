package com.manacommunity.api.groupbuy.service;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.groupbuy.dto.DemandItemRequest;
import com.manacommunity.api.groupbuy.dto.DemandItemResponse;
import com.manacommunity.api.groupbuy.entity.GroupDemandItem;
import com.manacommunity.api.groupbuy.repository.GroupDemandItemRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupDemandService {

    private final GroupDemandItemRepository demandRepo;

    @Transactional(readOnly = true)
    public Page<DemandItemResponse> getBoard(Long communityId, int page, int size, Long currentUserId) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return demandRepo.findByCommunityIdOrderByUpvotesDesc(communityId, pageable)
                .map(d -> toResponse(d, currentUserId));
    }

    @Transactional(readOnly = true)
    public DemandItemResponse getById(Long id, Long currentUserId) {
        return toResponse(find(id), currentUserId);
    }

    @Transactional
    public DemandItemResponse create(DemandItemRequest req, AppUser requestedBy, Community community) {
        GroupDemandItem item = GroupDemandItem.builder()
                .community(community)
                .requestedBy(requestedBy)
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .build();
        item.getUpvoterIds().add(requestedBy.getId());
        GroupDemandItem saved = demandRepo.save(item);
        log.info("DemandItem created id={} by user={}", saved.getId(), requestedBy.getId());
        return toResponse(saved, requestedBy.getId());
    }

    @Transactional
    public DemandItemResponse upvote(Long demandId, Long userId) {
        GroupDemandItem item = find(demandId);
        if (item.getUpvoterIds().contains(userId)) {
            throw new ManaCommunityException("You have already upvoted this demand",
                    HttpStatus.CONFLICT, "ALREADY_UPVOTED");
        }
        item.getUpvoterIds().add(userId);
        item.setUpvotes(item.getUpvotes() + 1);
        demandRepo.save(item);
        return toResponse(item, userId);
    }

    private GroupDemandItem find(Long id) {
        return demandRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandItem", id));
    }

    private DemandItemResponse toResponse(GroupDemandItem d, Long currentUserId) {
        return DemandItemResponse.builder()
                .id(d.getId())
                .title(d.getTitle())
                .description(d.getDescription())
                .category(d.getCategory())
                .requestedBy(d.getRequestedBy() != null ? d.getRequestedBy().getFullName() : null)
                .upvotes(d.getUpvotes())
                .targetUpvotes(d.getTargetUpvotes())
                .hasUpvoted(currentUserId != null && d.getUpvoterIds().contains(currentUserId))
                .status(d.getStatus().name())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
