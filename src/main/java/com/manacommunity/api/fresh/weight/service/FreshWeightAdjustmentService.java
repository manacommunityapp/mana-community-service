package com.manacommunity.api.fresh.weight.service;

import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.exception.InvalidStateTransitionException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.fresh.fulfilment.model.FreshFulfilmentItem;
import com.manacommunity.api.fresh.fulfilment.model.FulfilmentGroupStatus;
import com.manacommunity.api.fresh.fulfilment.repository.FreshFulfilmentItemRepository;
import com.manacommunity.api.fresh.weight.dto.RecordWeightRequest;
import com.manacommunity.api.fresh.weight.dto.WeightAdjustmentDto;
import com.manacommunity.api.fresh.weight.model.FreshWeightAdjustmentLog;
import com.manacommunity.api.fresh.weight.model.WeightAdjustmentStatus;
import com.manacommunity.api.fresh.weight.repository.FreshWeightAdjustmentLogRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreshWeightAdjustmentService {

    private final FreshFulfilmentItemRepository itemRepository;
    private final FreshWeightAdjustmentLogRepository weightLogRepository;
    private final AppUserRepository userRepository;

    private static final BigDecimal DEFAULT_MAX_TOLERANCE_PCT = new BigDecimal("15.00");

    @Transactional
    public WeightAdjustmentDto recordActualWeight(RecordWeightRequest req) {
        FreshFulfilmentItem item = itemRepository.findById(req.getFulfilmentItemId())
                .orElseThrow(() -> new ResourceNotFoundException("FreshFulfilmentItem", req.getFulfilmentItemId()));

        FulfilmentGroupStatus groupStatus = item.getFulfilmentGroup() != null ? item.getFulfilmentGroup().getStatus() : null;
        if (groupStatus == FulfilmentGroupStatus.DELIVERED || groupStatus == FulfilmentGroupStatus.CANCELLED) {
            throw new InvalidStateTransitionException("FreshFulfilmentItem", groupStatus.name(), "WEIGHT_ADJUSTED");
        }

        CommerceOrder order = item.getFulfilmentGroup().getOrder();
        AppUser customer = order.getBuyer();
        AppUser weighedBy = req.getWeighedByUserId() != null
                ? userRepository.findById(req.getWeighedByUserId()).orElse(null)
                : null;

        BigDecimal requestedG = resolveRequestedWeightG(item);
        BigDecimal actualG = req.getActualWeightG();
        BigDecimal diffG = actualG.subtract(requestedG);

        BigDecimal diffPct = BigDecimal.ZERO;
        if (requestedG.compareTo(BigDecimal.ZERO) > 0) {
            diffPct = diffG.divide(requestedG, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal originalAmount = item.getBilledPrice();
        BigDecimal priceRatio = requestedG.compareTo(BigDecimal.ZERO) > 0
                ? actualG.divide(requestedG, 4, RoundingMode.HALF_UP)
                : BigDecimal.ONE;

        BigDecimal adjustedAmount = originalAmount.multiply(priceRatio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal refundOrCharge = adjustedAmount.subtract(originalAmount).abs();

        WeightAdjustmentStatus status;
        if (diffPct.abs().compareTo(DEFAULT_MAX_TOLERANCE_PCT) > 0) {
            status = WeightAdjustmentStatus.PENDING_CUSTOMER_APPROVAL;
            log.warn("Item {} weight deviation {}% exceeds tolerance limit {}%",
                    item.getId(), diffPct, DEFAULT_MAX_TOLERANCE_PCT);
        } else if (diffG.compareTo(BigDecimal.ZERO) < 0) {
            status = WeightAdjustmentStatus.REFUNDED_TO_WALLET;
        } else if (diffG.compareTo(BigDecimal.ZERO) > 0) {
            status = WeightAdjustmentStatus.CHARGED_TO_WALLET;
        } else {
            status = WeightAdjustmentStatus.AUTO_ADJUSTED;
        }

        item.setIsWeightAdjusted(true);
        if ("KG".equalsIgnoreCase(item.getUnitOfMeasure())) {
            item.setPickedQuantity(actualG.divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP));
        } else {
            item.setPickedQuantity(actualG);
        }

        if (status != WeightAdjustmentStatus.PENDING_CUSTOMER_APPROVAL) {
            item.setBilledPrice(adjustedAmount);
        }
        itemRepository.save(item);

        String ref = "WADJ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        FreshWeightAdjustmentLog logEntry = FreshWeightAdjustmentLog.builder()
                .adjustmentReference(ref)
                .fulfilmentItem(item)
                .order(order)
                .customer(customer)
                .requestedWeightG(requestedG)
                .actualWeightG(actualG)
                .weightDifferenceG(diffG)
                .weightDifferencePct(diffPct)
                .maxTolerancePct(DEFAULT_MAX_TOLERANCE_PCT)
                .originalAmount(originalAmount)
                .adjustedAmount(adjustedAmount)
                .refundOrChargeAmount(refundOrCharge)
                .adjustmentStatus(status)
                .reason(req.getReason())
                .weighedByUser(weighedBy)
                .build();

        FreshWeightAdjustmentLog saved = weightLogRepository.save(logEntry);
        log.info("Recorded weight adjustment {} for item {}: {}g -> {}g (Diff: {}%, Status: {})",
                ref, item.getId(), requestedG, actualG, diffPct, status);

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<WeightAdjustmentDto> getAdjustmentsByOrder(Long orderId) {
        return weightLogRepository.findByOrderId(orderId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WeightAdjustmentDto> getAdjustmentsByItem(Long itemId) {
        return weightLogRepository.findByFulfilmentItemId(itemId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private BigDecimal resolveRequestedWeightG(FreshFulfilmentItem item) {
        if ("KG".equalsIgnoreCase(item.getUnitOfMeasure())) {
            return item.getRequestedQuantity().multiply(BigDecimal.valueOf(1000));
        }
        return item.getRequestedQuantity();
    }

    public WeightAdjustmentDto toDto(FreshWeightAdjustmentLog entry) {
        return WeightAdjustmentDto.builder()
                .id(entry.getId())
                .adjustmentReference(entry.getAdjustmentReference())
                .fulfilmentItemId(entry.getFulfilmentItem().getId())
                .orderId(entry.getOrder().getId())
                .orderNumber(entry.getOrder().getOrderNumber())
                .customerId(entry.getCustomer() != null ? entry.getCustomer().getId() : null)
                .customerName(entry.getCustomer() != null ? entry.getCustomer().getFullName() : null)
                .requestedWeightG(entry.getRequestedWeightG())
                .actualWeightG(entry.getActualWeightG())
                .weightDifferenceG(entry.getWeightDifferenceG())
                .weightDifferencePct(entry.getWeightDifferencePct())
                .maxTolerancePct(entry.getMaxTolerancePct())
                .originalAmount(entry.getOriginalAmount())
                .adjustedAmount(entry.getAdjustedAmount())
                .refundOrChargeAmount(entry.getRefundOrChargeAmount())
                .adjustmentStatus(entry.getAdjustmentStatus())
                .reason(entry.getReason())
                .weighedAt(entry.getWeighedAt())
                .build();
    }
}
