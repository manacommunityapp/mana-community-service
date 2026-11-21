package com.manacommunity.api.grouporder.service;

import com.manacommunity.api.grouporder.dto.*;
import com.manacommunity.api.grouporder.entity.*;
import com.manacommunity.api.grouporder.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupOrderService {

    private final GroupOrderRepository groupOrderRepository;
    private final GroupOrderContributionRepository contributionRepository;
    private final GroupOrderUpdateRepository updateRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public GroupOrder createGroupOrder(Long communityId, Long organizerId, CreateGroupOrderRequest req) {
        GroupOrder order = GroupOrder.builder()
                .communityId(communityId)
                .organizerId(organizerId)
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .sourceVendor(req.getSourceVendor())
                .itemName(req.getItemName())
                .unitPrice(req.getUnitPrice())
                .unitDescription(req.getUnitDescription())
                .minQuantity(req.getMinQuantity())
                .maxQuantity(req.getMaxQuantity())
                .closesAt(req.getClosesAt())
                .deliveryDate(req.getDeliveryDate())
                .deliveryPoint(req.getDeliveryPoint())
                .imageUrl(req.getImageUrl())
                .status("OPEN")
                .totalCommitted(0)
                .build();
        return groupOrderRepository.save(order);
    }

    @Transactional
    public GroupOrderContribution joinGroupOrder(Long groupOrderId, Long residentId, int quantity) {
        GroupOrder order = findOpenOrder(groupOrderId);
        if (order.getMaxQuantity() != null) {
            int available = order.getMaxQuantity() - order.getTotalCommitted();
            if (quantity > available) {
                throw new IllegalStateException("Requested quantity exceeds remaining capacity");
            }
        }

        GroupOrderContribution contribution = contributionRepository
                .findByGroupOrderIdAndResidentId(groupOrderId, residentId)
                .orElse(GroupOrderContribution.builder()
                        .groupOrderId(groupOrderId)
                        .residentId(residentId)
                        .build());

        int delta = quantity - contribution.getQuantity();
        contribution.setQuantity(quantity);
        contribution.setUnitPriceAtJoin(order.getUnitPrice());
        GroupOrderContribution saved = contributionRepository.save(contribution);

        order.setTotalCommitted(order.getTotalCommitted() + delta);
        if ("OPEN".equals(order.getStatus()) && order.getTotalCommitted() >= order.getMinQuantity()) {
            order.setStatus("MIN_REACHED");
            publishStatusEvent(order, "Minimum quantity reached — order can now be placed");
        }
        groupOrderRepository.save(order);

        return saved;
    }

    @Transactional
    public void leaveGroupOrder(Long groupOrderId, Long residentId) {
        GroupOrder order = groupOrderRepository.findById(groupOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Group order not found"));
        if ("ORDERED".equals(order.getStatus()) || "DELIVERED".equals(order.getStatus())) {
            throw new IllegalStateException("Cannot leave after order has been placed");
        }
        contributionRepository.findByGroupOrderIdAndResidentId(groupOrderId, residentId)
                .ifPresent(c -> {
                    order.setTotalCommitted(Math.max(0, order.getTotalCommitted() - c.getQuantity()));
                    contributionRepository.delete(c);
                    if ("MIN_REACHED".equals(order.getStatus())
                            && order.getTotalCommitted() < order.getMinQuantity()) {
                        order.setStatus("OPEN");
                    }
                    groupOrderRepository.save(order);
                });
    }

    @Transactional
    public GroupOrder closeAndOrder(Long groupOrderId, Long organizerId,
                                   CloseGroupOrderRequest req) {
        GroupOrder order = groupOrderRepository.findById(groupOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Group order not found"));
        if (!order.getOrganizerId().equals(organizerId)) {
            throw new IllegalStateException("Only the organizer can place the order");
        }
        if (!"MIN_REACHED".equals(order.getStatus()) && !"OPEN".equals(order.getStatus())) {
            throw new IllegalStateException("Order cannot be closed in status " + order.getStatus());
        }

        order.setFinalUnitPrice(req.getFinalUnitPrice());
        order.setVendorOrderRef(req.getVendorRef());
        if (req.getDeliveryDate() != null) order.setDeliveryDate(req.getDeliveryDate());
        order.setStatus("ORDERED");

        // Set total_amount for each contribution
        List<GroupOrderContribution> contributions = contributionRepository.findByGroupOrderId(groupOrderId);
        for (GroupOrderContribution c : contributions) {
            c.setTotalAmount(req.getFinalUnitPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
            contributionRepository.save(c);
        }

        GroupOrder saved = groupOrderRepository.save(order);
        publishStatusEvent(saved, "Order placed with vendor — delivery on " + order.getDeliveryDate());
        recordUpdate(groupOrderId, organizerId, "STATUS_CHANGE",
                "Order placed at ₹" + req.getFinalUnitPrice() + " per unit. Ref: " + req.getVendorRef());
        return saved;
    }

    @Transactional
    public void confirmCollection(Long groupOrderId, Long residentId) {
        GroupOrder order = groupOrderRepository.findById(groupOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Group order not found"));
        if (!"ORDERED".equals(order.getStatus()) && !"DELIVERED".equals(order.getStatus())) {
            throw new IllegalStateException("Order not yet placed");
        }
        contributionRepository.findByGroupOrderIdAndResidentId(groupOrderId, residentId)
                .ifPresent(c -> {
                    c.setCollectionStatus("COLLECTED");
                    contributionRepository.save(c);
                });

        // If all collected, mark delivered
        List<GroupOrderContribution> all = contributionRepository.findByGroupOrderId(groupOrderId);
        boolean allCollected = all.stream().allMatch(c -> "COLLECTED".equals(c.getCollectionStatus()));
        if (allCollected && "ORDERED".equals(order.getStatus())) {
            order.setStatus("DELIVERED");
            groupOrderRepository.save(order);
            publishStatusEvent(order, "All items collected — order complete");
        }
    }

    @Transactional
    public void cancel(Long groupOrderId, Long actorId, String reason) {
        GroupOrder order = groupOrderRepository.findById(groupOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Group order not found"));
        if ("DELIVERED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new IllegalStateException("Cannot cancel order in status " + order.getStatus());
        }

        // Mark contributions for refund
        List<GroupOrderContribution> contributions = contributionRepository.findByGroupOrderId(groupOrderId);
        for (GroupOrderContribution c : contributions) {
            if ("PAID".equals(c.getPaymentStatus())) {
                c.setPaymentStatus("REFUNDED");
                contributionRepository.save(c);
            }
        }

        order.setStatus("CANCELLED");
        groupOrderRepository.save(order);
        recordUpdate(groupOrderId, actorId, "STATUS_CHANGE", "Order cancelled: " + reason);
        publishStatusEvent(order, "Order cancelled: " + reason);
    }

    @Transactional
    public GroupOrderUpdate postUpdate(Long groupOrderId, Long organizerId,
                                      PostUpdateRequest req) {
        GroupOrder order = groupOrderRepository.findById(groupOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Group order not found"));
        if (!order.getOrganizerId().equals(organizerId)) {
            throw new IllegalStateException("Only the organizer can post updates");
        }
        return recordUpdate(groupOrderId, organizerId, req.getUpdateType(), req.getMessage());
    }

    public Page<GroupOrder> getOpenOrders(Long communityId, String category, String status, Pageable pageable) {
        if (category != null && status != null) {
            return groupOrderRepository.findByCommunityIdAndCategoryAndStatus(communityId, category, status, pageable);
        } else if (category != null) {
            return groupOrderRepository.findByCommunityIdAndCategory(communityId, category, pageable);
        } else if (status != null) {
            return groupOrderRepository.findByCommunityIdAndStatus(communityId, status, pageable);
        }
        return groupOrderRepository.findByCommunityId(communityId, pageable);
    }

    public List<GroupOrderContribution> getMyJoined(Long residentId) {
        return contributionRepository.findByResidentId(residentId);
    }

    public List<GroupOrder> getMyOrganised(Long organizerId) {
        return groupOrderRepository.findByOrganizerId(organizerId);
    }

    public GroupOrder getOrder(Long id) {
        return groupOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Group order not found"));
    }

    public List<GroupOrderUpdate> getUpdates(Long groupOrderId) {
        return updateRepository.findByGroupOrderIdOrderByPostedAtAsc(groupOrderId);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private GroupOrder findOpenOrder(Long groupOrderId) {
        GroupOrder order = groupOrderRepository.findById(groupOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Group order not found"));
        if ("ORDERED".equals(order.getStatus()) || "DELIVERED".equals(order.getStatus())
                || "CANCELLED".equals(order.getStatus())) {
            throw new IllegalStateException("Group order is no longer accepting contributions");
        }
        if (order.getClosesAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Group order has closed");
        }
        return order;
    }

    private GroupOrderUpdate recordUpdate(Long groupOrderId, Long postedBy,
                                         String updateType, String message) {
        GroupOrderUpdate update = GroupOrderUpdate.builder()
                .groupOrderId(groupOrderId)
                .updateType(updateType)
                .message(message)
                .postedBy(postedBy)
                .build();
        return updateRepository.save(update);
    }

    private void publishStatusEvent(GroupOrder order, String message) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/community/" + order.getCommunityId() + "/group-orders",
                    Map.of(
                            "groupOrderId", order.getId(),
                            "status", order.getStatus(),
                            "title", order.getTitle(),
                            "message", message,
                            "totalCommitted", order.getTotalCommitted()
                    )
            );
        } catch (Exception ex) {
            log.warn("WebSocket push failed for group order {}: {}", order.getId(), ex.getMessage());
        }
    }
}
