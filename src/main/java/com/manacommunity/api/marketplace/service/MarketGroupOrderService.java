package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.marketplace.dto.MarketGroupOrderDto;
import com.manacommunity.api.marketplace.dto.MarketGroupOrderParticipantDto;
import com.manacommunity.api.marketplace.entity.MarketGroupOrder;
import com.manacommunity.api.marketplace.entity.MarketGroupOrderParticipant;
import com.manacommunity.api.marketplace.entity.MarketGroupOrderTier;
import com.manacommunity.api.marketplace.repository.MarketGroupOrderParticipantRepository;
import com.manacommunity.api.marketplace.repository.MarketGroupOrderRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketGroupOrderService {

    private final MarketGroupOrderRepository groupOrderRepository;
    private final MarketGroupOrderParticipantRepository participantRepository;

    public Page<MarketGroupOrderDto.Response> getActiveGroupOrders(Long communityId, Pageable pageable) {
        return groupOrderRepository.findByCommunityIdAndStatus(communityId, MarketGroupOrder.GroupOrderStatus.ACTIVE, pageable)
                .map(this::toResponse);
    }

    public MarketGroupOrderDto.Response getById(Long id) {
        return groupOrderRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Group order not found with id: " + id));
    }

    @Transactional
    public MarketGroupOrderDto.Response create(MarketGroupOrderDto.Request req, AppUser organizer, Community community) {
        MarketGroupOrder order = MarketGroupOrder.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .basePrice(req.getBasePrice())
                .currentPrice(req.getBasePrice())
                .targetQuantity(req.getTargetQuantity())
                .currentQuantity(0)
                .expiresAt(req.getExpiresAt())
                .status(MarketGroupOrder.GroupOrderStatus.ACTIVE)
                .organizer(organizer)
                .community(community)
                .discountTiers(new ArrayList<>())
                .participants(new ArrayList<>())
                .build();

        if (req.getDiscountTiers() != null) {
            for (MarketGroupOrderDto.TierDto t : req.getDiscountTiers()) {
                MarketGroupOrderTier tier = MarketGroupOrderTier.builder()
                        .groupOrder(order)
                        .minQuantity(t.getMinQuantity())
                        .discountedPrice(t.getDiscountedPrice())
                        .discountPercent(t.getDiscountPercent())
                        .build();
                order.getDiscountTiers().add(tier);
            }
        }

        return toResponse(groupOrderRepository.save(order));
    }

    @Transactional
    public MarketGroupOrderParticipantDto.Response joinGroupOrder(Long groupOrderId, MarketGroupOrderParticipantDto.JoinRequest req, AppUser user) {
        MarketGroupOrder groupOrder = groupOrderRepository.findById(groupOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Group order not found with id: " + groupOrderId));

        if (groupOrder.getStatus() != MarketGroupOrder.GroupOrderStatus.ACTIVE) {
            throw new InvalidInputException("This group order is no longer accepting participants");
        }

        int newTotalQty = groupOrder.getCurrentQuantity() + req.getQuantity();
        groupOrder.setCurrentQuantity(newTotalQty);

        // Recalculate tier price
        BigDecimal newPrice = groupOrder.getBasePrice();
        if (groupOrder.getDiscountTiers() != null) {
            for (MarketGroupOrderTier tier : groupOrder.getDiscountTiers()) {
                if (newTotalQty >= tier.getMinQuantity()) {
                    if (tier.getDiscountedPrice().compareTo(newPrice) < 0) {
                        newPrice = tier.getDiscountedPrice();
                    }
                }
            }
        }
        groupOrder.setCurrentPrice(newPrice);

        BigDecimal totalCost = newPrice.multiply(BigDecimal.valueOf(req.getQuantity()));

        MarketGroupOrderParticipant participant = MarketGroupOrderParticipant.builder()
                .groupOrder(groupOrder)
                .user(user)
                .quantity(req.getQuantity())
                .lockedPrice(newPrice)
                .totalPaid(totalCost)
                .paymentStatus(MarketGroupOrderParticipant.PaymentStatus.PAID)
                .build();

        MarketGroupOrderParticipant saved = participantRepository.save(participant);
        groupOrderRepository.save(groupOrder);

        return MarketGroupOrderParticipantDto.Response.builder()
                .id(saved.getId())
                .groupOrderId(groupOrder.getId())
                .userId(user.getId())
                .userName(user.getFullName() != null ? user.getFullName() : user.getUsername())
                .quantity(saved.getQuantity())
                .lockedPrice(saved.getLockedPrice())
                .totalPaid(saved.getTotalPaid())
                .paymentStatus(saved.getPaymentStatus())
                .joinedAt(saved.getJoinedAt())
                .build();
    }

    private MarketGroupOrderDto.Response toResponse(MarketGroupOrder g) {
        List<MarketGroupOrderDto.TierDto> tierDtos = g.getDiscountTiers() != null
                ? g.getDiscountTiers().stream().map(t -> MarketGroupOrderDto.TierDto.builder()
                .minQuantity(t.getMinQuantity())
                .discountedPrice(t.getDiscountedPrice())
                .discountPercent(t.getDiscountPercent())
                .build()).collect(Collectors.toList())
                : List.of();

        return MarketGroupOrderDto.Response.builder()
                .id(g.getId())
                .title(g.getTitle())
                .description(g.getDescription())
                .category(g.getCategory())
                .basePrice(g.getBasePrice())
                .targetQuantity(g.getTargetQuantity())
                .currentQuantity(g.getCurrentQuantity())
                .currentPrice(g.getCurrentPrice())
                .expiresAt(g.getExpiresAt())
                .status(g.getStatus())
                .organizerId(g.getOrganizer().getId())
                .organizerName(g.getOrganizer().getFullName() != null ? g.getOrganizer().getFullName() : g.getOrganizer().getUsername())
                .communityId(g.getCommunity() != null ? g.getCommunity().getId() : null)
                .discountTiers(tierDtos)
                .participantsCount(g.getParticipants() != null ? g.getParticipants().size() : 0)
                .createdAt(g.getCreatedAt())
                .build();
    }
}
