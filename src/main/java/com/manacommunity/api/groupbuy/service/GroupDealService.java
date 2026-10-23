package com.manacommunity.api.groupbuy.service;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.groupbuy.dto.*;
import com.manacommunity.api.groupbuy.entity.GroupDeal;
import com.manacommunity.api.groupbuy.entity.GroupDealTier;
import com.manacommunity.api.groupbuy.entity.GroupOrder;
import com.manacommunity.api.groupbuy.repository.GroupDealRepository;
import com.manacommunity.api.groupbuy.repository.GroupOrderRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupDealService {

    private final GroupDealRepository dealRepo;
    private final GroupOrderRepository orderRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<GroupDealResponse> getDeals(Long communityId, String category, String search,
                                             String status, int page, int size, Long currentUserId) {
        GroupDeal.DealStatus dealStatus = parseStatus(status, GroupDeal.DealStatus.ACTIVE);
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<GroupDeal> deals;
        if (search != null && !search.isBlank()) {
            deals = dealRepo.searchActive(communityId, search, pageable);
        } else if (category != null && !category.isBlank() && !"All".equalsIgnoreCase(category)) {
            deals = dealRepo.findByCommunityIdAndStatusAndCategory(communityId, dealStatus, category, pageable);
        } else {
            deals = dealRepo.findByCommunityIdAndStatus(communityId, dealStatus, pageable);
        }
        return deals.map(d -> toResponse(d, currentUserId));
    }

    @Transactional(readOnly = true)
    public GroupDealResponse getById(Long id, Long currentUserId) {
        return toResponse(findDeal(id), currentUserId);
    }

    @Transactional(readOnly = true)
    public List<GroupDealResponse> getAlmostUnlocked(Long communityId, Long currentUserId) {
        return dealRepo.findAlmostUnlocked(communityId, PageRequest.of(0, 10))
                .stream().map(d -> toResponse(d, currentUserId)).toList();
    }

    @Transactional(readOnly = true)
    public List<GroupDealResponse> getEndingSoon(Long communityId, Long currentUserId) {
        LocalDateTime in48h = LocalDateTime.now().plusHours(48);
        return dealRepo.findEndingSoon(communityId, in48h, PageRequest.of(0, 10))
                .stream().map(d -> toResponse(d, currentUserId)).toList();
    }

    @Transactional(readOnly = true)
    public List<GroupDealResponse> getFestivalDeals(Long communityId, Long currentUserId) {
        return dealRepo.findFestivalDeals(communityId, PageRequest.of(0, 20))
                .stream().map(d -> toResponse(d, currentUserId)).toList();
    }

    @Transactional
    public GroupDealResponse create(GroupDealRequest req, AppUser creator, Community community) {
        GroupDeal deal = GroupDeal.builder()
                .community(community)
                .createdBy(creator)
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .subCategory(req.getSubCategory())
                .imageUrl(req.getImageUrl())
                .mrp(req.getMrp())
                .vendorName(req.getVendorName())
                .vendorRating(req.getVendorRating())
                .vendorVerified(Boolean.TRUE.equals(req.getVendorVerified()))
                .targetParticipants(req.getTargetParticipants())
                .endDate(req.getEndDate())
                .deliveryDate(req.getDeliveryDate())
                .isFestival(Boolean.TRUE.equals(req.getIsFestival()))
                .festivalName(req.getFestivalName())
                .tags(req.getTags() != null ? String.join(",", req.getTags()) : null)
                .pickupPoints(req.getPickupPoints() != null ? String.join("|", req.getPickupPoints()) : null)
                .build();

        for (int i = 0; i < req.getTiers().size(); i++) {
            GroupDealRequest.TierRequest tr = req.getTiers().get(i);
            GroupDealTier tier = GroupDealTier.builder()
                    .deal(deal)
                    .minQty(tr.getMinQty())
                    .maxQty(tr.getMaxQty())
                    .pricePerUnit(tr.getPricePerUnit())
                    .label(tr.getLabel())
                    .sortOrder(i)
                    .build();
            deal.getTiers().add(tier);
        }

        GroupDeal saved = dealRepo.save(deal);
        auditService.record(AuditAction.GROUP_DEAL_CREATED, AuditModule.GROUP_BUYING,
                "GroupDeal", String.valueOf(saved.getId()));
        log.info("GroupDeal created id={} community={}", saved.getId(), community.getId());
        return toResponse(saved, creator.getId());
    }

    @Transactional
    public GroupDealResponse updateStatus(Long dealId, String status, Long adminUserId) {
        GroupDeal deal = findDeal(dealId);
        GroupDeal.DealStatus newStatus = parseStatus(status, null);
        if (newStatus == null) {
            throw new ManaCommunityException("Invalid status: " + status, HttpStatus.BAD_REQUEST, "INVALID_STATUS");
        }
        deal.setStatus(newStatus);
        dealRepo.save(deal);
        auditService.record(AuditAction.GROUP_DEAL_UPDATED, AuditModule.GROUP_BUYING,
                "GroupDeal", String.valueOf(dealId), deal.getStatus().name(), status);
        return toResponse(deal, adminUserId);
    }

    @Transactional
    public GroupOrderResponse joinDeal(Long dealId, JoinDealRequest req, AppUser user, Community community) {
        GroupDeal deal = findDeal(dealId);

        if (deal.getStatus() != GroupDeal.DealStatus.ACTIVE) {
            throw new ManaCommunityException("This deal is no longer accepting participants",
                    HttpStatus.CONFLICT, "DEAL_NOT_ACTIVE");
        }
        if (deal.getEndDate().isBefore(LocalDateTime.now())) {
            throw new ManaCommunityException("This deal has expired", HttpStatus.CONFLICT, "DEAL_EXPIRED");
        }
        if (orderRepo.existsByDealIdAndUserId(dealId, user.getId())) {
            throw new ManaCommunityException("You have already joined this deal",
                    HttpStatus.CONFLICT, "ALREADY_JOINED");
        }

        BigDecimal price = resolvePrice(deal, deal.getCurrentParticipants() + req.getQuantity());
        BigDecimal total = price.multiply(BigDecimal.valueOf(req.getQuantity()));

        GroupOrder order = GroupOrder.builder()
                .deal(deal)
                .user(user)
                .community(community)
                .orderNumber("GB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .quantity(req.getQuantity())
                .pricePerUnit(price)
                .totalAmount(total)
                .fulfillment(parseEnum(GroupOrder.FulfillmentType.class, req.getFulfillment(),
                        GroupOrder.FulfillmentType.PICKUP))
                .paymentMethod(parseEnum(GroupOrder.PaymentMethod.class, req.getPaymentMethod(),
                        GroupOrder.PaymentMethod.UPI))
                .pickupPoint(req.getPickupPoint())
                .deliveryAddress(req.getDeliveryAddress())
                .qrCode("QR-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .build();

        orderRepo.save(order);

        deal.setCurrentParticipants(deal.getCurrentParticipants() + req.getQuantity());
        if (deal.getCurrentParticipants() >= deal.getTargetParticipants()
                && deal.getStatus() == GroupDeal.DealStatus.ACTIVE) {
            deal.setStatus(GroupDeal.DealStatus.FUNDED);
            log.info("GroupDeal id={} reached target — status → FUNDED", deal.getId());
        }
        dealRepo.save(deal);

        auditService.record(AuditAction.GROUP_ORDER_PLACED, AuditModule.GROUP_BUYING,
                "GroupOrder", order.getOrderNumber());
        return toOrderResponse(order);
    }

    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        GroupOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("GroupOrder", orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You can only cancel your own orders");
        }
        if (order.getStatus() == GroupOrder.OrderStatus.DELIVERED) {
            throw new ManaCommunityException("Delivered orders cannot be cancelled",
                    HttpStatus.CONFLICT, "ORDER_DELIVERED");
        }
        order.setStatus(GroupOrder.OrderStatus.CANCELLED);
        orderRepo.save(order);

        GroupDeal deal = order.getDeal();
        deal.setCurrentParticipants(Math.max(0, deal.getCurrentParticipants() - order.getQuantity()));
        if (deal.getStatus() == GroupDeal.DealStatus.FUNDED
                && deal.getCurrentParticipants() < deal.getTargetParticipants()) {
            deal.setStatus(GroupDeal.DealStatus.ACTIVE);
        }
        dealRepo.save(deal);
    }

    @Transactional(readOnly = true)
    public List<GroupOrderResponse> getMyOrders(Long userId) {
        return orderRepo.findByUserIdOrderByOrderedAtDesc(userId)
                .stream().map(this::toOrderResponse).toList();
    }

    public void expireOverdueDeals() {
        List<GroupDeal> overdue = dealRepo.findByStatusAndEndDateBefore(
                GroupDeal.DealStatus.ACTIVE, LocalDateTime.now());
        for (GroupDeal deal : overdue) {
            deal.setStatus(GroupDeal.DealStatus.EXPIRED);
            dealRepo.save(deal);
            log.info("GroupDeal id={} expired", deal.getId());
        }
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private GroupDealResponse toResponse(GroupDeal d, Long currentUserId) {
        boolean hasJoined = currentUserId != null && orderRepo.existsByDealIdAndUserId(d.getId(), currentUserId);

        BigDecimal currentPrice = resolvePrice(d, d.getCurrentParticipants());
        BigDecimal nextPrice = null;
        Integer nextUnitsNeeded = null;
        GroupDealTier nextTier = d.getTiers().stream()
                .filter(t -> t.getMinQty() > d.getCurrentParticipants())
                .findFirst().orElse(null);
        if (nextTier != null) {
            nextPrice = nextTier.getPricePerUnit();
            nextUnitsNeeded = nextTier.getMinQty() - d.getCurrentParticipants();
        }

        return GroupDealResponse.builder()
                .id(d.getId())
                .title(d.getTitle())
                .description(d.getDescription())
                .category(d.getCategory())
                .subCategory(d.getSubCategory())
                .imageUrl(d.getImageUrl())
                .mrp(d.getMrp())
                .vendor(d.getVendorName())
                .vendorId(d.getVendor() != null ? d.getVendor().getId() : null)
                .vendorRating(d.getVendorRating())
                .vendorVerified(d.getVendorVerified())
                .targetParticipants(d.getTargetParticipants())
                .currentParticipants(d.getCurrentParticipants())
                .endDate(d.getEndDate() != null ? d.getEndDate().toString() : null)
                .deliveryDate(d.getDeliveryDate() != null ? d.getDeliveryDate().toString() : null)
                .status(d.getStatus().name())
                .isFestival(d.getIsFestival())
                .festivalName(d.getFestivalName())
                .tags(d.getTags() != null ? Arrays.asList(d.getTags().split(",")) : List.of())
                .pickupPoints(d.getPickupPoints() != null ? Arrays.asList(d.getPickupPoints().split("\\|")) : List.of())
                .tiers(d.getTiers().stream().map(t -> GroupDealResponse.TierResponse.builder()
                        .minQty(t.getMinQty()).maxQty(t.getMaxQty())
                        .pricePerUnit(t.getPricePerUnit()).label(t.getLabel())
                        .build()).collect(Collectors.toList()))
                .hasJoined(hasJoined)
                .currentTierPrice(currentPrice)
                .nextTierPrice(nextPrice)
                .nextTierUnitsNeeded(nextUnitsNeeded)
                .createdAt(d.getCreatedAt())
                .build();
    }

    private GroupOrderResponse toOrderResponse(GroupOrder o) {
        return GroupOrderResponse.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .dealId(o.getDeal().getId())
                .dealTitle(o.getDeal().getTitle())
                .quantity(o.getQuantity())
                .pricePerUnit(o.getPricePerUnit())
                .totalAmount(o.getTotalAmount())
                .status(o.getStatus().name())
                .fulfillment(o.getFulfillment().name())
                .paymentMethod(o.getPaymentMethod().name())
                .pickupPoint(o.getPickupPoint())
                .deliveryAddress(o.getDeliveryAddress())
                .qrCode(o.getQrCode())
                .reviewed(o.getReviewed())
                .orderedAt(o.getOrderedAt())
                .vendorName(o.getDeal().getVendorName())
                .build();
    }

    private BigDecimal resolvePrice(GroupDeal deal, int participantCount) {
        return deal.getTiers().stream()
                .filter(t -> participantCount >= t.getMinQty()
                        && (t.getMaxQty() == null || participantCount <= t.getMaxQty()))
                .map(GroupDealTier::getPricePerUnit)
                .findFirst()
                .orElse(deal.getTiers().isEmpty() ? BigDecimal.ZERO : deal.getTiers().get(0).getPricePerUnit());
    }

    private GroupDeal findDeal(Long id) {
        return dealRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GroupDeal", id));
    }

    private GroupDeal.DealStatus parseStatus(String s, GroupDeal.DealStatus fallback) {
        if (s == null || s.isBlank()) return fallback;
        try { return GroupDeal.DealStatus.valueOf(s); } catch (IllegalArgumentException e) { return fallback; }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> cls, String value, E fallback) {
        if (value == null || value.isBlank()) return fallback;
        try { return Enum.valueOf(cls, value); } catch (IllegalArgumentException e) { return fallback; }
    }
}
