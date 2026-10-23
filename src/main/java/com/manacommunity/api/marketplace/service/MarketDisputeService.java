package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.marketplace.dto.MarketDisputeRequest;
import com.manacommunity.api.marketplace.dto.MarketDisputeResponse;
import com.manacommunity.api.marketplace.entity.MarketDispute;
import com.manacommunity.api.marketplace.entity.MarketOrder;
import com.manacommunity.api.marketplace.repository.MarketDisputeRepository;
import com.manacommunity.api.marketplace.repository.MarketOrderRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketDisputeService {

    private final MarketDisputeRepository disputeRepository;
    private final MarketOrderRepository orderRepository;

    public Page<MarketDisputeResponse> getCommunityDisputes(Long communityId, MarketDispute.DisputeStatus status, Pageable pageable) {
        return disputeRepository.findByCommunityIdAndStatus(communityId, status != null ? status : MarketDispute.DisputeStatus.OPEN, pageable)
                .map(this::toResponse);
    }

    public List<MarketDisputeResponse> getMyDisputes(Long userId) {
        return disputeRepository.findByRaisedByIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MarketDisputeResponse raiseDispute(MarketDisputeRequest req, AppUser user, Community community) {
        MarketOrder order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + req.getOrderId()));

        MarketDispute dispute = MarketDispute.builder()
                .order(order)
                .raisedBy(user)
                .community(community != null ? community : order.getCommunity())
                .reason(req.getReason())
                .description(req.getDescription())
                .status(MarketDispute.DisputeStatus.OPEN)
                .build();

        return toResponse(disputeRepository.save(dispute));
    }

    @Transactional
    public MarketDisputeResponse resolveDispute(Long disputeId, MarketDisputeRequest.ResolveRequest req, AppUser admin) {
        MarketDispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found with id: " + disputeId));

        dispute.setStatus(req.getStatus());
        dispute.setResolutionNotes(req.getResolutionNotes());
        dispute.setResolvedBy(admin);
        dispute.setResolvedAt(LocalDateTime.now());

        return toResponse(disputeRepository.save(dispute));
    }

    private MarketDisputeResponse toResponse(MarketDispute d) {
        return MarketDisputeResponse.builder()
                .id(d.getId())
                .orderId(d.getOrder().getId())
                .orderNumber(d.getOrder().getOrderNumber())
                .raisedById(d.getRaisedBy().getId())
                .raisedByName(d.getRaisedBy().getFullName() != null ? d.getRaisedBy().getFullName() : d.getRaisedBy().getUsername())
                .communityId(d.getCommunity() != null ? d.getCommunity().getId() : null)
                .reason(d.getReason())
                .description(d.getDescription())
                .status(d.getStatus())
                .resolutionNotes(d.getResolutionNotes())
                .resolvedByName(d.getResolvedBy() != null ? d.getResolvedBy().getFullName() : null)
                .resolvedAt(d.getResolvedAt())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
