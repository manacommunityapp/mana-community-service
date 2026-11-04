package com.manacommunity.api.fresh.recall.service;

import com.manacommunity.api.exception.InvalidStateTransitionException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.fresh.fulfilment.model.FreshFulfilmentItem;
import com.manacommunity.api.fresh.fulfilment.repository.FreshFulfilmentItemRepository;
import com.manacommunity.api.fresh.recall.dto.InitiateRecallRequest;
import com.manacommunity.api.fresh.recall.dto.ProductRecallDto;
import com.manacommunity.api.fresh.recall.dto.RecallOrderDto;
import com.manacommunity.api.fresh.recall.model.*;
import com.manacommunity.api.fresh.recall.repository.FreshProductRecallOrderRepository;
import com.manacommunity.api.fresh.recall.repository.FreshProductRecallRepository;
import com.manacommunity.api.freshkitchen.model.BatchStatus;
import com.manacommunity.api.freshkitchen.model.FoodProductionBatch;
import com.manacommunity.api.freshkitchen.repository.FoodProductionBatchRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.vendor.commerce.repository.VendorProductRepository;
import com.manacommunity.api.commerce.core.service.CommerceWalletService;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreshRecallService {

    private final FreshProductRecallRepository recallRepository;
    private final FreshProductRecallOrderRepository recallOrderRepository;
    private final FreshFulfilmentItemRepository fulfilmentItemRepository;
    private final VendorProductRepository vendorProductRepository;
    private final FoodProductionBatchRepository batchRepository;
    private final AppUserRepository userRepository;
    private final CommerceWalletService walletService;
    private final AuditService auditService;

    @Transactional
    public ProductRecallDto initiateRecall(InitiateRecallRequest req) {
        VendorProduct product = vendorProductRepository.findById(req.getVendorProductId())
                .orElseThrow(() -> new ResourceNotFoundException("VendorProduct", req.getVendorProductId()));

        FoodProductionBatch batch = null;
        if (req.getBatchId() != null) {
            batch = batchRepository.findById(req.getBatchId()).orElse(null);
            if (batch != null) {
                batch.setBatchStatus(BatchStatus.QUARANTINED);
                batchRepository.save(batch);
                log.warn("Batch {} automatically QUARANTINED due to product recall initiation", batch.getBatchNumber());
            }
        }

        AppUser initiator = userRepository.findById(req.getInitiatedByUserId())
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", req.getInitiatedByUserId()));

        String ref = "RCL-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))
                + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();

        FreshProductRecall recall = FreshProductRecall.builder()
                .recallReference(ref)
                .vendorProduct(product)
                .batch(batch)
                .severity(req.getSeverity())
                .reason(req.getReason())
                .detailedRootCause(req.getDetailedRootCause())
                .initiatedByUser(initiator)
                .status(RecallStatus.INITIATED)
                .build();

        FreshProductRecall savedRecall = recallRepository.save(recall);

        // Find affected fulfilment items
        List<FreshFulfilmentItem> affectedItems;
        if (batch != null) {
            affectedItems = fulfilmentItemRepository.findByBatchId(batch.getId());
        } else {
            affectedItems = Collections.emptyList();
        }

        List<FreshProductRecallOrder> recallOrders = new ArrayList<>();
        BigDecimal totalUnits = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        Set<Long> uniqueCustomers = new HashSet<>();

        for (FreshFulfilmentItem item : affectedItems) {
            BigDecimal qty = item.getPickedQuantity() != null ? item.getPickedQuantity() : item.getRequestedQuantity();
            BigDecimal refund = item.getBilledPrice();

            totalUnits = totalUnits.add(qty);
            totalRefund = totalRefund.add(refund);
            if (item.getFulfilmentGroup().getOrder().getBuyer() != null) {
                uniqueCustomers.add(item.getFulfilmentGroup().getOrder().getBuyer().getId());
            }

            FreshProductRecallOrder ro = FreshProductRecallOrder.builder()
                    .recall(savedRecall)
                    .order(item.getFulfilmentGroup().getOrder())
                    .customer(item.getFulfilmentGroup().getOrder().getBuyer())
                    .fulfilmentItem(item)
                    .quantity(qty)
                    .refundAmount(refund)
                    .customerNotified(false)
                    .refundProcessed(false)
                    .build();

            recallOrders.add(ro);
        }

        if (!recallOrders.isEmpty()) {
            recallOrderRepository.saveAll(recallOrders);
        }

        savedRecall.setAffectedOrders(recallOrders);
        savedRecall.setAffectedOrdersCount(recallOrders.size());
        savedRecall.setAffectedCustomersCount(uniqueCustomers.size());
        savedRecall.setTotalUnitsRecalled(totalUnits);
        savedRecall.setTotalRefundAmount(totalRefund);
        recallRepository.save(savedRecall);

        if (auditService != null) {
            auditService.record(AuditAction.FRESH_RECALL_INITIATED, AuditModule.FRESH_COMMERCE,
                    "FreshProductRecall", ref, null, req.getSeverity() + ":" + req.getReason());
        }

        log.info("Initiated product recall {}: affected {} orders, {} customers, total refund ₹{}",
                ref, recallOrders.size(), uniqueCustomers.size(), totalRefund);

        return toDto(savedRecall);
    }

    @Transactional
    public ProductRecallDto processRecallRefunds(Long recallId) {
        FreshProductRecall recall = recallRepository.findById(recallId)
                .orElseThrow(() -> new ResourceNotFoundException("FreshProductRecall", recallId));

        if (recall.getStatus() == RecallStatus.COMPLETED) {
            log.info("Recall {} is already COMPLETED. Returning idempotently.", recall.getRecallReference());
            return toDto(recall);
        }

        List<FreshProductRecallOrder> orders = recallOrderRepository.findByRecallId(recallId);
        Instant now = Instant.now();

        List<FreshProductRecallOrder> toUpdate = new ArrayList<>();
        for (FreshProductRecallOrder order : orders) {
            if (!Boolean.TRUE.equals(order.getRefundProcessed())) {
                String walletTxnRef = null;
                if (order.getCustomer() != null && order.getRefundAmount() != null && order.getRefundAmount().compareTo(BigDecimal.ZERO) > 0 && walletService != null) {
                    try {
                        walletService.getOrCreateWallet(order.getCustomer());
                        String refundRef = "RECALL-" + recall.getRecallReference() + "-" + order.getId();
                        walletService.refund(order.getCustomer(), order.getRefundAmount(), refundRef);
                        walletTxnRef = "TXN-" + refundRef;
                        log.info("Credited recall refund ₹{} to user {} for recall {}",
                                order.getRefundAmount(), order.getCustomer().getId(), recall.getRecallReference());
                    } catch (Exception ex) {
                        log.error("Failed to credit wallet for recall order {}: {}", order.getId(), ex.getMessage());
                    }
                }
                order.setWalletTransactionRef(walletTxnRef);
                order.setCustomerNotified(true);
                order.setRefundProcessed(true);
                order.setNotificationSentAt(now);
                toUpdate.add(order);
            }
        }

        if (!toUpdate.isEmpty()) {
            recallOrderRepository.saveAll(toUpdate);
        }

        recall.setStatus(RecallStatus.COMPLETED);
        recallRepository.save(recall);

        if (auditService != null) {
            auditService.record(AuditAction.FRESH_RECALL_REFUNDED, AuditModule.FRESH_COMMERCE,
                    "FreshProductRecall", recall.getRecallReference(), RecallStatus.INITIATED.name(), RecallStatus.COMPLETED.name());
        }

        log.info("Completed recall {} refunds for {} orders (processed {})",
                recall.getRecallReference(), orders.size(), toUpdate.size());
        return toDto(recall);
    }

    @Transactional(readOnly = true)
    public List<ProductRecallDto> getAllRecalls() {
        return recallRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductRecallDto getRecallById(Long recallId) {
        FreshProductRecall recall = recallRepository.findById(recallId)
                .orElseThrow(() -> new ResourceNotFoundException("FreshProductRecall", recallId));
        return toDto(recall);
    }

    public ProductRecallDto toDto(FreshProductRecall r) {
        List<RecallOrderDto> orderDtos = r.getAffectedOrders() != null
                ? r.getAffectedOrders().stream().map(this::toOrderDto).collect(Collectors.toList())
                : Collections.emptyList();

        return ProductRecallDto.builder()
                .id(r.getId())
                .recallReference(r.getRecallReference())
                .batchId(r.getBatch() != null ? r.getBatch().getId() : null)
                .batchNumber(r.getBatch() != null ? r.getBatch().getBatchNumber() : null)
                .vendorProductId(r.getVendorProduct() != null ? r.getVendorProduct().getId() : null)
                .productName(r.getVendorProduct() != null ? r.getVendorProduct().getName() : null)
                .severity(r.getSeverity())
                .reason(r.getReason())
                .detailedRootCause(r.getDetailedRootCause())
                .initiatedByUserId(r.getInitiatedByUser() != null ? r.getInitiatedByUser().getId() : null)
                .initiatedByUserName(r.getInitiatedByUser() != null ? r.getInitiatedByUser().getFullName() : null)
                .initiatedAt(r.getInitiatedAt())
                .status(r.getStatus())
                .affectedOrdersCount(r.getAffectedOrdersCount())
                .affectedCustomersCount(r.getAffectedCustomersCount())
                .totalUnitsRecalled(r.getTotalUnitsRecalled())
                .totalRefundAmount(r.getTotalRefundAmount())
                .affectedOrders(orderDtos)
                .createdAt(r.getCreatedAt())
                .build();
    }

    public RecallOrderDto toOrderDto(FreshProductRecallOrder ro) {
        return RecallOrderDto.builder()
                .id(ro.getId())
                .recallId(ro.getRecall().getId())
                .orderId(ro.getOrder().getId())
                .orderNumber(ro.getOrder().getOrderNumber())
                .customerId(ro.getCustomer() != null ? ro.getCustomer().getId() : null)
                .customerName(ro.getCustomer() != null ? ro.getCustomer().getFullName() : null)
                .fulfilmentItemId(ro.getFulfilmentItem() != null ? ro.getFulfilmentItem().getId() : null)
                .quantity(ro.getQuantity())
                .refundAmount(ro.getRefundAmount())
                .customerNotified(ro.getCustomerNotified())
                .refundProcessed(ro.getRefundProcessed())
                .walletTransactionRef(ro.getWalletTransactionRef())
                .notificationSentAt(ro.getNotificationSentAt())
                .createdAt(ro.getCreatedAt())
                .build();
    }
}
