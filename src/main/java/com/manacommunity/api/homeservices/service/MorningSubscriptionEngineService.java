package com.manacommunity.api.homeservices.service;

import com.manacommunity.api.homeservices.entity.HsRecurringPlan;
import com.manacommunity.api.homeservices.entity.MorningDeliveryInstance;
import com.manacommunity.api.homeservices.entity.MorningSubOverride;
import com.manacommunity.api.homeservices.repository.HsRecurringPlanRepository;
import com.manacommunity.api.homeservices.repository.MorningDeliveryInstanceRepository;
import com.manacommunity.api.homeservices.repository.MorningSubOverrideRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Early-Morning Subscription Delivery Engine (5:30 AM dispatch at scale).
 * Handles 23:59 cutoff freeze, daily vacation/qty overrides, wallet pre-holds,
 * tower/floor-sorted vendor manifests, and 09:30 AM failed-delivery auto-refunds.
 */
@Service
@RequiredArgsConstructor
public class MorningSubscriptionEngineService {

    public static final LocalTime MIDNIGHT_CUTOFF_TIME = LocalTime.of(23, 59);

    private final HsRecurringPlanRepository planRepo;
    private final MorningSubOverrideRepository overrideRepo;
    private final MorningDeliveryInstanceRepository instanceRepo;

    @Builder
    public record OverrideResult(
            Long planId,
            LocalDate effectiveDeliveryDate,
            boolean rolledPastCutoff,
            int qty,
            boolean paused,
            String message
    ) {}

    @Builder
    public record VendorRouteManifest(
            Long vendorId,
            LocalDate deliveryDate,
            int totalFlats,
            int lowBalancePausedCount,
            Map<String, Integer> skuPackingTotals,
            List<MorningDeliveryInstance> orderedStops
    ) {}

    @Transactional
    public OverrideResult upsertOverride(
            Long planId,
            LocalDate requestedDate,
            Integer overrideQty,
            boolean paused,
            LocalDateTime now
    ) {
        LocalDateTime ref = now != null ? now : LocalDateTime.now();
        LocalDate tomorrow = ref.toLocalDate().plusDays(1);
        LocalDate effectiveDate = requestedDate;
        boolean rolled = false;

        // Enforce strict 23:59 cutoff freeze for D+1 dispatch
        if (!requestedDate.isAfter(tomorrow) && !ref.toLocalTime().isBefore(MIDNIGHT_CUTOFF_TIME)) {
            effectiveDate = tomorrow.plusDays(1);
            rolled = true;
        }

        int finalQty = Math.max(0, overrideQty != null ? overrideQty : 1);
        final LocalDate target = effectiveDate;
        MorningSubOverride ov = overrideRepo.findByPlanIdAndDeliveryDate(planId, target)
                .orElseGet(() -> MorningSubOverride.builder().planId(planId).deliveryDate(target).build());

        ov.setOverrideQty(paused ? 0 : finalQty);
        ov.setPaused(paused);
        ov.setUpdatedAt(Instant.now());
        overrideRepo.save(ov);

        return OverrideResult.builder()
                .planId(planId)
                .effectiveDeliveryDate(target)
                .rolledPastCutoff(rolled)
                .qty(ov.getOverrideQty())
                .paused(paused)
                .message(rolled
                        ? "Past 23:59 cutoff for tomorrow; override applied to " + target
                        : "Subscription override locked for " + target)
                .build();
    }

    /**
     * Freezes D+1 active subscriptions, evaluates vacation windows & daily overrides, places
     * idempotent wallet holds, and emits a tower/floor-sorted vendor route manifest.
     */
    @Transactional
    public VendorRouteManifest freezeAndBuildVendorManifest(
            Long vendorId,
            LocalDate deliveryDate,
            BiPredicate<Long, BigDecimal> walletReserveFn
    ) {
        List<HsRecurringPlan> activePlans = planRepo.findByStatusAndNextServiceDateLessThanEqual("ACTIVE", deliveryDate);
        int lowBalanceCount = 0;

        for (HsRecurringPlan plan : activePlans) {
            if (vendorId != null && plan.getVendorId() != null && !vendorId.equals(plan.getVendorId())) {
                continue;
            }
            if (instanceRepo.findByPlanIdAndDeliveryDate(plan.getId(), deliveryDate).isPresent()) {
                continue; // Idempotent guard
            }
            boolean onVacation = plan.getVacationFrom() != null && plan.getVacationUntil() != null
                    && !deliveryDate.isBefore(plan.getVacationFrom())
                    && !deliveryDate.isAfter(plan.getVacationUntil());
            if (onVacation) {
                continue;
            }

            MorningSubOverride ov = overrideRepo.findByPlanIdAndDeliveryDate(plan.getId(), deliveryDate).orElse(null);
            if (ov != null && (Boolean.TRUE.equals(ov.getPaused()) || ov.getOverrideQty() <= 0)) {
                continue;
            }

            int qty = (ov != null) ? ov.getOverrideQty() : (plan.getDefaultQty() != null ? plan.getDefaultQty() : 1);
            BigDecimal unitPrice = plan.getUnitPrice() != null ? plan.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(qty));

            boolean reserved = walletReserveFn == null || walletReserveFn.test(plan.getUserId(), amount);
            String walletStatus = reserved ? "RESERVED" : "LOW_BALANCE_PAUSED";
            String deliveryStatus = reserved ? "FROZEN_FOR_DISPATCH" : "SKIPPED";
            if (!reserved) lowBalanceCount++;

            instanceRepo.save(MorningDeliveryInstance.builder()
                    .planId(plan.getId())
                    .communityId(plan.getCommunityId())
                    .vendorId(plan.getVendorId())
                    .userId(plan.getUserId())
                    .skuCode(plan.getSkuCode() != null ? plan.getSkuCode() : "MILK_500ML")
                    .towerBlock(plan.getTowerBlock() != null ? plan.getTowerBlock() : "A")
                    .floorNumber(plan.getFloorNumber() != null ? plan.getFloorNumber() : 0)
                    .flatNumber(plan.getFlatNumber() != null ? plan.getFlatNumber() : "000")
                    .deliveryDate(deliveryDate)
                    .qty(qty)
                    .amount(amount)
                    .walletHoldRef("sub:" + plan.getId() + ":" + deliveryDate)
                    .walletStatus(walletStatus)
                    .deliveryStatus(deliveryStatus)
                    .failureReason(reserved ? null : "Insufficient wallet balance at freeze")
                    .build());
        }

        List<MorningDeliveryInstance> rawStops = instanceRepo
                .findByVendorIdAndDeliveryDateOrderByCommunityIdAscTowerBlockAscFloorNumberDescFlatNumberAsc(vendorId, deliveryDate);

        // Ensure spatial sort order in-memory too for mock tests
        Comparator<MorningDeliveryInstance> spatialSort = Comparator
                .comparing(MorningDeliveryInstance::getCommunityId, Comparator.nullsFirst(Long::compareTo))
                .thenComparing(MorningDeliveryInstance::getTowerBlock, Comparator.nullsFirst(String::compareToIgnoreCase))
                .thenComparing(MorningDeliveryInstance::getFloorNumber, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MorningDeliveryInstance::getFlatNumber, Comparator.nullsFirst(String::compareToIgnoreCase));

        List<MorningDeliveryInstance> dispatchStops = rawStops.stream()
                .filter(s -> "FROZEN_FOR_DISPATCH".equals(s.getDeliveryStatus()))
                .sorted(spatialSort)
                .toList();

        Map<String, Integer> skuTotals = dispatchStops.stream().collect(
                Collectors.groupingBy(
                        MorningDeliveryInstance::getSkuCode,
                        Collectors.summingInt(MorningDeliveryInstance::getQty)
                ));

        return VendorRouteManifest.builder()
                .vendorId(vendorId)
                .deliveryDate(deliveryDate)
                .totalFlats(dispatchStops.size())
                .lowBalancePausedCount(lowBalanceCount)
                .skuPackingTotals(skuTotals)
                .orderedStops(dispatchStops)
                .build();
    }

    @Transactional
    public MorningDeliveryInstance settleDeliveryOutcome(Long instanceId, boolean delivered, String failureReason) {
        MorningDeliveryInstance inst = instanceRepo.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery instance not found: " + instanceId));

        if (delivered) {
            inst.setDeliveryStatus("DELIVERED");
            inst.setWalletStatus("CAPTURED");
            inst.setFailureReason(null);
        } else {
            inst.setDeliveryStatus(failureReason != null && failureReason.contains("STOCK") ? "OUT_OF_STOCK" : "SKIPPED");
            if ("RESERVED".equals(inst.getWalletStatus())) {
                inst.setWalletStatus("AUTO_REFUNDED");
            }
            inst.setFailureReason(failureReason != null ? failureReason : "Undelivered before morning SLA");
        }
        inst.setSettledAt(Instant.now());
        return instanceRepo.save(inst);
    }

    /**
     * Sweeps lingering undelivered instances past the 09:30 AM SLA into AUTO_REFUNDED.
     */
    @Transactional
    public int sweepUnmarkedToAutoRefund(LocalDate deliveryDate) {
        List<MorningDeliveryInstance> stale = instanceRepo.findByDeliveryDateAndDeliveryStatus(deliveryDate, "FROZEN_FOR_DISPATCH");
        int refunded = 0;
        for (MorningDeliveryInstance inst : stale) {
            inst.setDeliveryStatus("SKIPPED");
            if ("RESERVED".equals(inst.getWalletStatus())) {
                inst.setWalletStatus("AUTO_REFUNDED");
                refunded++;
            }
            inst.setFailureReason("09:30 AM auto-refund sweep: delivery not confirmed by vendor");
            inst.setSettledAt(Instant.now());
            instanceRepo.save(inst);
        }
        return refunded;
    }
}
