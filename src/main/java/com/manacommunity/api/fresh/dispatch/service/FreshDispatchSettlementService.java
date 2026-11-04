package com.manacommunity.api.fresh.dispatch.service;

import com.manacommunity.api.fresh.dispatch.dto.*;
import com.manacommunity.api.fresh.dispatch.model.*;
import com.manacommunity.api.fresh.dispatch.repository.*;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.vendor.commerce.repository.VendorProductRepository;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import com.manacommunity.api.exception.InvalidStateTransitionException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FreshDispatchSettlementService {

    private final FreshPurchaseOrderRepository poRepository;
    private final FreshPurchaseOrderItemRepository poItemRepository;
    private final FreshDeliveryBatchRepository deliveryBatchRepository;
    private final FreshDeliveryItemRepository deliveryItemRepository;
    private final FreshVendorSettlementRepository settlementRepository;
    private final VmsVendorRepository vendorRepository;
    private final VendorProductRepository vendorProductRepository;
    private final CommunityRepository communityRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;

    // ----- Purchase Orders (Demand Aggregation) -----

    @Transactional
    public PurchaseOrderDto createPurchaseOrder(CreatePurchaseOrderRequest req) {
        VmsVendor vendor = vendorRepository.findById(req.getVendorId())
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + req.getVendorId()));

        String poNumber = "PO-FRESH-" + req.getTargetDeliveryDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();

        FreshPurchaseOrder po = FreshPurchaseOrder.builder()
                .poNumber(poNumber)
                .vendor(vendor)
                .targetDeliveryDate(req.getTargetDeliveryDate())
                .status(PurchaseOrderStatus.SENT_TO_VENDOR)
                .totalAmount(BigDecimal.ZERO)
                .notes(req.getNotes())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        List<FreshPurchaseOrderItem> items = new ArrayList<>();

        if (req.getItems() != null) {
            for (CreatePurchaseOrderRequest.ItemRequest ir : req.getItems()) {
                VendorProduct product = vendorProductRepository.findById(ir.getVendorProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + ir.getVendorProductId()));

                BigDecimal itemTotal = ir.getQuantity().multiply(ir.getUnitCost()).setScale(2, java.math.RoundingMode.HALF_UP);
                total = total.add(itemTotal);

                FreshPurchaseOrderItem item = FreshPurchaseOrderItem.builder()
                        .purchaseOrder(po)
                        .vendorProduct(product)
                        .quantity(ir.getQuantity())
                        .unitCost(ir.getUnitCost())
                        .totalCost(itemTotal)
                        .demandSource(ir.getDemandSource() != null ? ir.getDemandSource() : DemandSource.RETAIL_ORDER)
                        .build();

                items.add(item);
            }
        }

        po.setTotalAmount(total.setScale(2, java.math.RoundingMode.HALF_UP));
        po.setItems(items);
        poRepository.save(po);

        return toPurchaseOrderDto(po);
    }

    @Transactional
    public PurchaseOrderDto updatePurchaseOrderStatus(Long poId, PurchaseOrderStatus status) {
        FreshPurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("PO not found: " + poId));
        po.setStatus(status);
        poRepository.save(po);
        return toPurchaseOrderDto(po);
    }

    public List<PurchaseOrderDto> getPurchaseOrdersByVendor(Long vendorId) {
        return poRepository.findByVendorIdOrderByTargetDeliveryDateDesc(vendorId).stream()
                .map(this::toPurchaseOrderDto)
                .collect(Collectors.toList());
    }

    // ----- Delivery Dispatch & Route Optimization -----

    @Transactional
    public DeliveryBatchDto createDeliveryBatch(CreateDeliveryBatchRequest req) {
        Community community = communityRepository.findById(req.getCommunityId())
                .orElseThrow(() -> new IllegalArgumentException("Community not found: " + req.getCommunityId()));

        AppUser runner = null;
        if (req.getAssignedRunnerId() != null) {
            runner = userRepository.findById(req.getAssignedRunnerId()).orElse(null);
        }

        String batchCode = "RUN-" + req.getDeliveryDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-C" + community.getId() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        FreshDeliveryBatch batch = FreshDeliveryBatch.builder()
                .batchCode(batchCode)
                .community(community)
                .deliveryDate(req.getDeliveryDate())
                .handlingClass(req.getHandlingClass() != null ? req.getHandlingClass() : HandlingClass.CHILLED_COLD_CHAIN)
                .assignedRunner(runner)
                .status(DeliveryBatchStatus.PLANNED)
                .build();

        List<FreshDeliveryItem> items = new ArrayList<>();
        if (req.getItems() != null) {
            for (CreateDeliveryBatchRequest.DeliveryItemRequest ir : req.getItems()) {
                FreshDeliveryItem item = FreshDeliveryItem.builder()
                        .deliveryBatch(batch)
                        .commerceOrderId(ir.getCommerceOrderId())
                        .customerName(ir.getCustomerName())
                        .tower(ir.getTower())
                        .flatNo(ir.getFlatNo())
                        .sortSequence(ir.getSortSequence() != null ? ir.getSortSequence() : 0)
                        .handoverStatus(HandoverStatus.PENDING)
                        .build();
                items.add(item);
            }
        }

        batch.setItems(items);
        deliveryBatchRepository.save(batch);

        return toDeliveryBatchDto(batch);
    }

    @Transactional
    public DeliveryBatchDto updateDeliveryBatchStatus(Long batchId, DeliveryBatchStatus status) {
        FreshDeliveryBatch batch = deliveryBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery batch not found: " + batchId));
        batch.setStatus(status);
        if (status == DeliveryBatchStatus.IN_TRANSIT && batch.getDispatchedAt() == null) {
            batch.setDispatchedAt(Instant.now());
        } else if (status == DeliveryBatchStatus.COMPLETED && batch.getCompletedAt() == null) {
            batch.setCompletedAt(Instant.now());
        }
        deliveryBatchRepository.save(batch);
        return toDeliveryBatchDto(batch);
    }

    @Transactional
    public DeliveryItemDto updateHandover(Long itemId, UpdateHandoverRequest req) {
        FreshDeliveryItem item = deliveryItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery item not found: " + itemId));
        item.setHandoverStatus(req.getHandoverStatus());
        if (req.getProofImageUrl() != null) {
            item.setProofImageUrl(req.getProofImageUrl());
        }
        if (req.getNotes() != null) {
            item.setNotes(req.getNotes());
        }
        if (req.getHandoverStatus() == HandoverStatus.DELIVERED) {
            item.setDeliveredAt(Instant.now());
        }
        deliveryItemRepository.save(item);
        return toDeliveryItemDto(item);
    }

    public List<DeliveryBatchDto> getDeliveryBatches(Long communityId, LocalDate date) {
        return deliveryBatchRepository.findByCommunityIdAndDeliveryDate(communityId, date).stream()
                .map(this::toDeliveryBatchDto)
                .collect(Collectors.toList());
    }

    // ----- Vendor Settlements -----

    @Transactional
    public VendorSettlementDto createSettlement(CreateSettlementRequest req) {
        VmsVendor vendor = vendorRepository.findById(req.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", req.getVendorId()));

        String ref = "SETTLE-FRESH-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-V" + vendor.getId() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        BigDecimal grossSales = req.getGrossSalesAmount() != null ? req.getGrossSalesAmount() : BigDecimal.ZERO;
        BigDecimal manaCommission = req.getManaCommissionAmount() != null
                ? req.getManaCommissionAmount()
                : grossSales.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deliveryFees = req.getDeliveryFeesCollected() != null ? req.getDeliveryFeesCollected() : BigDecimal.ZERO;
        BigDecimal gatewayFees = req.getGatewayFeesDeducted() != null ? req.getGatewayFeesDeducted() : BigDecimal.ZERO;

        // Statutory deductions: TDS Section 194-O (1%), GST on commission (18%), and quality/weight clawbacks
        BigDecimal tdsDeducted = grossSales.multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gstOnCommission = manaCommission.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal clawbacks = req.getRefundClawbacksAmount() != null ? req.getRefundClawbacksAmount() : BigDecimal.ZERO;

        BigDecimal netPayable = grossSales
                .subtract(manaCommission)
                .subtract(gstOnCommission)
                .subtract(tdsDeducted)
                .subtract(clawbacks)
                .add(deliveryFees)
                .subtract(gatewayFees);

        FreshVendorSettlement s = FreshVendorSettlement.builder()
                .settlementReference(ref)
                .vendor(vendor)
                .settlementPeriodStart(req.getSettlementPeriodStart())
                .settlementPeriodEnd(req.getSettlementPeriodEnd())
                .grossSalesAmount(grossSales)
                .manaCommissionAmount(manaCommission)
                .deliveryFeesCollected(deliveryFees)
                .gatewayFeesDeducted(gatewayFees)
                .tdsDeductedAmount(tdsDeducted)
                .gstOnCommissionAmount(gstOnCommission)
                .refundClawbacksAmount(clawbacks)
                .netPayableAmount(netPayable)
                .status(SettlementStatus.PENDING)
                .build();

        settlementRepository.save(s);
        return toSettlementDto(s);
    }

    @Transactional
    public VendorSettlementDto approveSettlement(Long settlementId) {
        FreshVendorSettlement s = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("FreshVendorSettlement", settlementId));
        if (s.getStatus() != SettlementStatus.PENDING) {
            throw new InvalidStateTransitionException("FreshVendorSettlement", s.getStatus().name(), SettlementStatus.APPROVED.name());
        }
        s.setStatus(SettlementStatus.APPROVED);
        settlementRepository.save(s);

        if (auditService != null) {
            auditService.record(AuditAction.FRESH_SETTLEMENT_APPROVED, AuditModule.FRESH_COMMERCE,
                    "FreshVendorSettlement", s.getSettlementReference(), SettlementStatus.PENDING.name(), SettlementStatus.APPROVED.name());
        }

        return toSettlementDto(s);
    }

    @Transactional
    public VendorSettlementDto processSettlement(Long settlementId, ProcessSettlementRequest req) {
        FreshVendorSettlement s = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("FreshVendorSettlement", settlementId));
        if (s.getStatus() != SettlementStatus.APPROVED) {
            throw new InvalidStateTransitionException("FreshVendorSettlement", s.getStatus().name(), SettlementStatus.PROCESSED.name());
        }
        s.setStatus(SettlementStatus.PROCESSED);
        s.setPayoutUtrReference(req.getPayoutUtrReference());
        s.setProcessedAt(Instant.now());
        settlementRepository.save(s);

        if (auditService != null) {
            auditService.record(AuditAction.FRESH_SETTLEMENT_PROCESSED, AuditModule.FRESH_COMMERCE,
                    "FreshVendorSettlement", s.getSettlementReference(), SettlementStatus.APPROVED.name(), SettlementStatus.PROCESSED.name());
        }

        return toSettlementDto(s);
    }

    public List<VendorSettlementDto> getSettlementsByVendor(Long vendorId) {
        return settlementRepository.findByVendorIdOrderBySettlementPeriodEndDesc(vendorId).stream()
                .map(this::toSettlementDto)
                .collect(Collectors.toList());
    }

    public List<VendorSettlementDto> getSettlementsByStatus(SettlementStatus status) {
        return settlementRepository.findByStatus(status).stream()
                .map(this::toSettlementDto)
                .collect(Collectors.toList());
    }

    // ----- DTO Mappers -----

    private PurchaseOrderDto toPurchaseOrderDto(FreshPurchaseOrder po) {
        List<PurchaseOrderItemDto> items = po.getItems().stream()
                .map(i -> PurchaseOrderItemDto.builder()
                        .id(i.getId())
                        .vendorProductId(i.getVendorProduct().getId())
                        .productName(i.getVendorProduct().getName())
                        .quantity(i.getQuantity())
                        .unitCost(i.getUnitCost())
                        .totalCost(i.getTotalCost())
                        .demandSource(i.getDemandSource())
                        .build())
                .collect(Collectors.toList());

        return PurchaseOrderDto.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .vendorId(po.getVendor().getId())
                .vendorBusinessName(po.getVendor().getBusinessName())
                .targetDeliveryDate(po.getTargetDeliveryDate())
                .status(po.getStatus())
                .totalAmount(po.getTotalAmount())
                .notes(po.getNotes())
                .createdAt(po.getCreatedAt())
                .items(items)
                .build();
    }

    private DeliveryBatchDto toDeliveryBatchDto(FreshDeliveryBatch b) {
        List<DeliveryItemDto> items = b.getItems().stream()
                .map(this::toDeliveryItemDto)
                .collect(Collectors.toList());

        return DeliveryBatchDto.builder()
                .id(b.getId())
                .batchCode(b.getBatchCode())
                .communityId(b.getCommunity().getId())
                .communityName(b.getCommunity().getName())
                .deliveryDate(b.getDeliveryDate())
                .handlingClass(b.getHandlingClass())
                .assignedRunnerId(b.getAssignedRunner() != null ? b.getAssignedRunner().getId() : null)
                .assignedRunnerName(b.getAssignedRunner() != null ? b.getAssignedRunner().getFullName() : null)
                .status(b.getStatus())
                .dispatchedAt(b.getDispatchedAt())
                .completedAt(b.getCompletedAt())
                .items(items)
                .build();
    }

    private DeliveryItemDto toDeliveryItemDto(FreshDeliveryItem i) {
        return DeliveryItemDto.builder()
                .id(i.getId())
                .commerceOrderId(i.getCommerceOrderId())
                .customerName(i.getCustomerName())
                .tower(i.getTower())
                .flatNo(i.getFlatNo())
                .sortSequence(i.getSortSequence())
                .handoverStatus(i.getHandoverStatus())
                .deliveredAt(i.getDeliveredAt())
                .proofImageUrl(i.getProofImageUrl())
                .notes(i.getNotes())
                .build();
    }

    private VendorSettlementDto toSettlementDto(FreshVendorSettlement s) {
        return VendorSettlementDto.builder()
                .id(s.getId())
                .settlementReference(s.getSettlementReference())
                .vendorId(s.getVendor().getId())
                .vendorBusinessName(s.getVendor().getBusinessName())
                .settlementPeriodStart(s.getSettlementPeriodStart())
                .settlementPeriodEnd(s.getSettlementPeriodEnd())
                .grossSalesAmount(s.getGrossSalesAmount())
                .manaCommissionAmount(s.getManaCommissionAmount())
                .deliveryFeesCollected(s.getDeliveryFeesCollected())
                .gatewayFeesDeducted(s.getGatewayFeesDeducted())
                .tdsDeductedAmount(s.getTdsDeductedAmount())
                .gstOnCommissionAmount(s.getGstOnCommissionAmount())
                .refundClawbacksAmount(s.getRefundClawbacksAmount())
                .netPayableAmount(s.getNetPayableAmount())
                .status(s.getStatus())
                .payoutUtrReference(s.getPayoutUtrReference())
                .processedAt(s.getProcessedAt())
                .createdAt(s.getCreatedAt())
                .build();
    }
}