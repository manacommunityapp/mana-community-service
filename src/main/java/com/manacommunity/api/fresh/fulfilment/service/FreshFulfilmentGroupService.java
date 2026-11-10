package com.manacommunity.api.fresh.fulfilment.service;

import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.model.CommerceOrderItem;
import com.manacommunity.api.commerce.core.repository.CommerceOrderItemRepository;
import com.manacommunity.api.commerce.core.repository.CommerceOrderRepository;
import com.manacommunity.api.exception.HandoverLockoutException;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.InvalidStateTransitionException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.fresh.catalog.model.DeliveryMode;
import com.manacommunity.api.fresh.catalog.model.FreshProductCatalogProfile;
import com.manacommunity.api.fresh.catalog.model.FulfilmentOwnerType;
import com.manacommunity.api.fresh.catalog.repository.FreshProductCatalogProfileRepository;
import com.manacommunity.api.fresh.fulfilment.dto.*;
import com.manacommunity.api.fresh.fulfilment.model.*;
import com.manacommunity.api.fresh.fulfilment.repository.*;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.vendor.commerce.repository.VendorProductRepository;
import com.manacommunity.api.vendor.entity.VmsVendor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreshFulfilmentGroupService {

    private final FreshFulfilmentGroupRepository groupRepository;
    private final FreshFulfilmentItemRepository itemRepository;
    private final CommerceOrderRepository orderRepository;
    private final CommerceOrderItemRepository orderItemRepository;
    private final VendorProductRepository vendorProductRepository;
    private final FreshProductCatalogProfileRepository catalogProfileRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public List<FulfilmentGroupDto> splitOrderIntoFulfilmentGroups(Long orderId) {
        CommerceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("CommerceOrder", orderId));

        List<FreshFulfilmentGroup> existingGroups = groupRepository.findByOrderId(orderId);
        if (!existingGroups.isEmpty()) {
            return existingGroups.stream().map(this::toGroupDto).collect(Collectors.toList());
        }

        List<CommerceOrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        if (orderItems.isEmpty()) {
            throw new IllegalStateException("Cannot split order without items: " + orderId);
        }

        // Group order items by (Vendor, FulfilmentOwnerType, DeliveryMode)
        Map<GroupPartitionKey, List<CommerceOrderItem>> partitions = new LinkedHashMap<>();

        for (CommerceOrderItem item : orderItems) {
            VendorProduct vp = resolveVendorProduct(item);
            FreshProductCatalogProfile profile = vp != null
                    ? catalogProfileRepository.findByVendorProductId(vp.getId()).orElse(null)
                    : null;

            Long vendorId = null;
            if (vp != null && vp.getVendor() != null) {
                vendorId = vp.getVendor().getId();
            } else if (order.getVendorId() != null) {
                try {
                    vendorId = Long.parseLong(order.getVendorId());
                } catch (NumberFormatException ignored) {}
            }

            FulfilmentOwnerType ownerType = FulfilmentOwnerType.VENDOR;
            DeliveryMode deliveryMode = DeliveryMode.VENDOR_DELIVERY;
            boolean coldChain = false;
            DietaryCategory dietaryCategory = DietaryCategory.VEGETARIAN;

            if (profile != null) {
                if (profile.getFulfilmentOwnerType() != null) {
                    ownerType = profile.getFulfilmentOwnerType();
                }
                if (profile.getDeliveryMode() != null) {
                    deliveryMode = profile.getDeliveryMode();
                }
                if (profile.getStorageInstruction() != null && profile.getStorageInstruction().toLowerCase().contains("cold")) {
                    coldChain = true;
                }
                if (profile.getCapabilityType() != null) {
                    dietaryCategory = profile.getCapabilityType().toDietaryCategory();
                }
            }

            GroupPartitionKey key = new GroupPartitionKey(vendorId, ownerType, deliveryMode, coldChain, dietaryCategory);
            partitions.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        List<FreshFulfilmentGroup> createdGroups = new ArrayList<>();
        int groupIndex = 1;

        for (Map.Entry<GroupPartitionKey, List<CommerceOrderItem>> entry : partitions.entrySet()) {
            GroupPartitionKey key = entry.getKey();
            List<CommerceOrderItem> items = entry.getValue();

            VmsVendor vendor = null;
            if (items.get(0).getProductId() != null) {
                VendorProduct vp = resolveVendorProduct(items.get(0));
                if (vp != null) {
                    vendor = vp.getVendor();
                }
            }

            String otp = String.format("%06d", 100000 + RANDOM.nextInt(900000));
            String qrToken = "FFG-" + UUID.randomUUID().toString();
            String groupRef = "GRP-" + order.getOrderNumber() + "-" + groupIndex;

            BigDecimal subtotal = items.stream()
                    .map(CommerceOrderItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            FreshFulfilmentGroup group = FreshFulfilmentGroup.builder()
                    .groupReference(groupRef)
                    .order(order)
                    .groupNumber(groupIndex++)
                    .vendor(vendor)
                    .fulfilmentOwnerType(key.ownerType())
                    .deliveryMode(key.deliveryMode())
                    .status(FulfilmentGroupStatus.CREATED)
                    .requiresColdChain(key.requiresColdChain())
                    .dietaryCategory(key.dietaryCategory())
                    .handoverOtp(otp)
                    .qrCodeToken(qrToken)
                    .subtotalAmount(subtotal)
                    .totalWeightG(BigDecimal.ZERO)
                    .build();

            FreshFulfilmentGroup savedGroup = groupRepository.save(group);

            List<FreshFulfilmentItem> fulfilmentItems = new ArrayList<>();
            for (CommerceOrderItem orderItem : items) {
                VendorProduct vp = resolveVendorProduct(orderItem);
                BigDecimal qty = BigDecimal.valueOf(orderItem.getQuantity() != null ? orderItem.getQuantity() : 1);

                FreshFulfilmentItem fItem = FreshFulfilmentItem.builder()
                        .fulfilmentGroup(savedGroup)
                        .orderItem(orderItem)
                        .vendorProduct(vp)
                        .requestedQuantity(qty)
                        .pickedQuantity(qty)
                        .unitOfMeasure(orderItem.getPackSize() != null ? orderItem.getPackSize() : "UNIT")
                        .unitPrice(orderItem.getUnitPrice())
                        .billedPrice(orderItem.getTotalPrice())
                        .status(FulfilmentItemStatus.PENDING)
                        .build();

                fulfilmentItems.add(fItem);
            }

            itemRepository.saveAll(fulfilmentItems);
            savedGroup.setItems(fulfilmentItems);
            createdGroups.add(savedGroup);

            log.info("Order {} split into fulfilment group {} (Owner: {}, Mode: {})",
                    order.getOrderNumber(), groupRef, key.ownerType(), key.deliveryMode());
        }

        return createdGroups.stream().map(this::toGroupDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FulfilmentGroupDto> getFulfilmentGroupsByOrder(Long orderId) {
        return getFulfilmentGroupsByOrder(orderId, null);
    }

    @Transactional(readOnly = true)
    public List<FulfilmentGroupDto> getFulfilmentGroupsByOrder(Long orderId, UserPrincipal principal) {
        if (principal != null) {
            boolean isAdmin = principal.getAuthorities() != null && principal.getAuthorities().stream().anyMatch(a ->
                    "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
            if (!isAdmin) {
                CommerceOrder order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("CommerceOrder", orderId));
                boolean isBuyer = order.getBuyer() != null && principal.getId() != null
                        && principal.getId().equals(order.getBuyer().getId());
                boolean isVendor = principal.getAuthorities() != null && principal.getAuthorities().stream().anyMatch(a ->
                        "ROLE_VENDOR".equals(a.getAuthority()));
                if (!isBuyer && !isVendor) {
                    throw new AccessDeniedException("Access denied to order " + orderId);
                }
            }
        }
        return groupRepository.findByOrderId(orderId).stream()
                .map(g -> toGroupDto(g, principal))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FulfilmentGroupDto> getFulfilmentGroupsByVendor(Long vendorId) {
        return groupRepository.findByVendorId(vendorId).stream()
                .map(this::toGroupDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FulfilmentGroupDto getFulfilmentGroupById(Long groupId) {
        return getFulfilmentGroupById(groupId, null);
    }

    @Transactional(readOnly = true)
    public FulfilmentGroupDto getFulfilmentGroupById(Long groupId, UserPrincipal principal) {
        FreshFulfilmentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("FreshFulfilmentGroup", groupId));
        if (principal != null) {
            boolean isAdmin = principal.getAuthorities() != null && principal.getAuthorities().stream().anyMatch(a ->
                    "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
            boolean isBuyer = group.getOrder() != null && group.getOrder().getBuyer() != null
                    && principal.getId() != null && principal.getId().equals(group.getOrder().getBuyer().getId());
            boolean isVendor = principal.getAuthorities() != null && principal.getAuthorities().stream().anyMatch(a ->
                    "ROLE_VENDOR".equals(a.getAuthority()));
            boolean isRunner = principal.getAuthorities() != null && principal.getAuthorities().stream().anyMatch(a ->
                    "ROLE_DELIVERY_PARTNER".equals(a.getAuthority()));
            if (!isAdmin && !isBuyer && !isVendor && !isRunner) {
                throw new AccessDeniedException("Access denied to fulfilment group " + groupId);
            }
        }
        return toGroupDto(group, principal);
    }

    @Transactional
    public FulfilmentGroupDto updateStatus(Long groupId, UpdateFulfilmentGroupStatusRequest req) {
        FreshFulfilmentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("FreshFulfilmentGroup", groupId));

        if (group.getStatus() == FulfilmentGroupStatus.CANCELLED) {
            throw new InvalidStateTransitionException("FreshFulfilmentGroup", "CANCELLED", req.getStatus().name());
        }
        if (group.getStatus() == FulfilmentGroupStatus.DELIVERED && req.getStatus() != FulfilmentGroupStatus.DELIVERED) {
            throw new InvalidStateTransitionException("FreshFulfilmentGroup", "DELIVERED", req.getStatus().name());
        }

        group.setStatus(req.getStatus());
        if (req.getTrackingReference() != null) {
            group.setTrackingReference(req.getTrackingReference());
        }

        Instant now = Instant.now();
        if (req.getStatus() == FulfilmentGroupStatus.DISPATCHED && group.getDispatchedAt() == null) {
            group.setDispatchedAt(now);
        } else if (req.getStatus() == FulfilmentGroupStatus.DELIVERED && group.getDeliveredAt() == null) {
            group.setDeliveredAt(now);
            if (group.getItems() != null) {
                group.getItems().forEach(it -> it.setStatus(FulfilmentItemStatus.PACKED));
            }
        }

        groupRepository.save(group);
        log.info("Fulfilment group {} updated to status {}", group.getGroupReference(), req.getStatus());
        return toGroupDto(group);
    }

    @Transactional
    public FulfilmentGroupDto verifyHandover(Long groupId, VerifyHandoverRequest req) {
        FreshFulfilmentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("FreshFulfilmentGroup", groupId));

        if (Boolean.TRUE.equals(group.getIsHandoverLocked()) ||
                (group.getFailedOtpAttempts() != null && group.getFailedOtpAttempts() >= 5)) {
            throw new HandoverLockoutException(group.getGroupReference(),
                    group.getFailedOtpAttempts() != null ? group.getFailedOtpAttempts() : 5, 5);
        }

        if (group.getStatus() == FulfilmentGroupStatus.CANCELLED) {
            throw new InvalidStateTransitionException("FreshFulfilmentGroup", "CANCELLED", "DELIVERED");
        }

        if (group.getStatus() == FulfilmentGroupStatus.DELIVERED) {
            log.info("Fulfilment group {} is already DELIVERED. Returning idempotently.", group.getGroupReference());
            return toGroupDto(group);
        }

        boolean otpMatches = req.getOtp() != null && !req.getOtp().isBlank()
                && group.getHandoverOtp() != null && group.getHandoverOtp().trim().equals(req.getOtp().trim());
        boolean qrMatches = req.getQrCodeToken() != null && !req.getQrCodeToken().isBlank()
                && group.getQrCodeToken() != null && group.getQrCodeToken().trim().equals(req.getQrCodeToken().trim());

        if (!otpMatches && !qrMatches) {
            int attempts = (group.getFailedOtpAttempts() != null ? group.getFailedOtpAttempts() : 0) + 1;
            group.setFailedOtpAttempts(attempts);
            if (attempts >= 5) {
                group.setIsHandoverLocked(true);
                groupRepository.save(group);
                log.warn("Handover locked out for group {} after {} failed attempts", group.getGroupReference(), attempts);
                throw new HandoverLockoutException(group.getGroupReference(), attempts, 5);
            }
            groupRepository.save(group);
            throw new InvalidInputException("Invalid OTP or QR token for handover verification (attempt " + attempts + "/5)");
        }

        group.setFailedOtpAttempts(0);
        group.setStatus(FulfilmentGroupStatus.DELIVERED);
        group.setDeliveredAt(Instant.now());
        if (group.getItems() != null) {
            group.getItems().forEach(it -> it.setStatus(FulfilmentItemStatus.PACKED));
        }

        groupRepository.save(group);
        log.info("Fulfilment group {} successfully verified and DELIVERED via handover", group.getGroupReference());
        return toGroupDto(group);
    }

    private VendorProduct resolveVendorProduct(CommerceOrderItem item) {
        if (item.getProductId() == null) return null;
        try {
            Long vpId = Long.parseLong(item.getProductId());
            return vendorProductRepository.findById(vpId).orElse(null);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public FulfilmentGroupDto toGroupDto(FreshFulfilmentGroup group) {
        return toGroupDto(group, null);
    }

    public FulfilmentGroupDto toGroupDto(FreshFulfilmentGroup group, UserPrincipal principal) {
        List<FulfilmentItemDto> itemDtos = group.getItems() != null
                ? group.getItems().stream().map(this::toItemDto).collect(Collectors.toList())
                : Collections.emptyList();

        boolean canSeeOtp = false;
        if (principal == null) {
            canSeeOtp = true; // Fallback for unit tests and internal calls
        } else {
            boolean isAdmin = principal.getAuthorities() != null && principal.getAuthorities().stream().anyMatch(a ->
                    "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
            boolean isBuyer = group.getOrder() != null && group.getOrder().getBuyer() != null
                    && principal.getId() != null && principal.getId().equals(group.getOrder().getBuyer().getId());
            canSeeOtp = isAdmin || isBuyer;
        }

        Long communityId = (group.getOrder() != null && group.getOrder().getCommunity() != null)
                ? group.getOrder().getCommunity().getId()
                : null;

        return FulfilmentGroupDto.builder()
                .id(group.getId())
                .groupReference(group.getGroupReference())
                .orderId(group.getOrder() != null ? group.getOrder().getId() : null)
                .orderNumber(group.getOrder() != null ? group.getOrder().getOrderNumber() : null)
                .communityId(communityId)
                .groupNumber(group.getGroupNumber())
                .vendorId(group.getVendor() != null ? group.getVendor().getId() : null)
                .vendorBusinessName(group.getVendor() != null ? group.getVendor().getBusinessName() : null)
                .fulfilmentOwnerType(group.getFulfilmentOwnerType())
                .deliveryMode(group.getDeliveryMode())
                .status(group.getStatus())
                .fulfilmentLocationId(group.getFulfilmentLocation() != null ? group.getFulfilmentLocation().getId() : null)
                .fulfilmentLocationName(group.getFulfilmentLocation() != null ? group.getFulfilmentLocation().getName() : null)
                .targetDeliveryTime(group.getTargetDeliveryTime())
                .dispatchedAt(group.getDispatchedAt())
                .deliveredAt(group.getDeliveredAt())
                .trackingReference(group.getTrackingReference())
                .handoverOtp(canSeeOtp ? group.getHandoverOtp() : null)
                .qrCodeToken(canSeeOtp ? group.getQrCodeToken() : null)
                .subtotalAmount(group.getSubtotalAmount())
                .totalWeightG(group.getTotalWeightG())
                .requiresColdChain(group.getRequiresColdChain())
                .dietaryCategory(group.getDietaryCategory())
                .failedOtpAttempts(group.getFailedOtpAttempts())
                .isHandoverLocked(group.getIsHandoverLocked())
                .items(itemDtos)
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    public FulfilmentItemDto toItemDto(FreshFulfilmentItem item) {
        return FulfilmentItemDto.builder()
                .id(item.getId())
                .fulfilmentGroupId(item.getFulfilmentGroup().getId())
                .orderItemId(item.getOrderItem().getId())
                .vendorProductId(item.getVendorProduct() != null ? item.getVendorProduct().getId() : null)
                .productName(item.getVendorProduct() != null ? item.getVendorProduct().getName() : item.getOrderItem().getTitle())
                .batchId(item.getBatch() != null ? item.getBatch().getId() : null)
                .batchNumber(item.getBatch() != null ? item.getBatch().getBatchNumber() : null)
                .requestedQuantity(item.getRequestedQuantity())
                .pickedQuantity(item.getPickedQuantity())
                .unitOfMeasure(item.getUnitOfMeasure())
                .unitPrice(item.getUnitPrice())
                .billedPrice(item.getBilledPrice())
                .isWeightAdjusted(item.getIsWeightAdjusted())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .build();
    }

    private record GroupPartitionKey(
            Long vendorId,
            FulfilmentOwnerType ownerType,
            DeliveryMode deliveryMode,
            boolean requiresColdChain,
            DietaryCategory dietaryCategory
    ) {}
}
