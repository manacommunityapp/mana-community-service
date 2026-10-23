package com.manacommunity.api.service;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryPickingService {

    private final InventoryPickingRepository pickingRepo;
    private final InventoryPickingTypeRepository pickingTypeRepo;
    private final InventoryMoveLineRepository moveLineRepo;
    private final InventoryProductRepository productRepo;
    private final InventoryLocationRepository locationRepo;
    private final InventoryStockQuantRepository quantRepo;
    private final InventorySequenceRepository seqRepo;
    private final StockLedgerEntryRepository ledgerRepo;
    private final AuditService auditService;

    // ═══════════════════════════════════════════════════════════════════════
    // DASHBOARD STATS
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<PickingTypeStatsResponse> getPickingTypeStats() {
        return pickingTypeRepo.findAllByOrderByCodeAsc().stream().map(pt ->
            new PickingTypeStatsResponse(
                pt.getId(), pt.getName(), pt.getCode().name(), pt.getWarehouse().getId(),
                pickingRepo.countToProcess(pt.getId()),
                pickingRepo.countLate(pt.getId()),
                pickingRepo.countBackorders(pt.getId())
            )
        ).toList();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PICKING CRUD
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<PickingResponse> getAllPickings() {
        return pickingRepo.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PickingResponse getPickingById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public PickingResponse createPicking(PickingRequest req) {
        InventoryPickingType pt = pickingTypeRepo.findById(req.pickingTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("InventoryPickingType", req.pickingTypeId()));
        InventoryLocation src = locationRepo.findById(req.locationId())
                .orElseThrow(() -> new ResourceNotFoundException("Source Location", req.locationId()));
        InventoryLocation dest = locationRepo.findById(req.locationDestId())
                .orElseThrow(() -> new ResourceNotFoundException("Dest Location", req.locationDestId()));

        String name = nextSequence(pt.getSequencePrefix());

        InventoryPicking picking = InventoryPicking.builder()
                .name(name)
                .state(InventoryPicking.PickingState.DRAFT)
                .pickingType(pt)
                .location(src)
                .locationDest(dest)
                .scheduledDate(req.scheduledDate())
                .origin(req.origin())
                .build();

        InventoryPicking saved = pickingRepo.save(picking);

        if (req.moveLines() != null) {
            for (MoveLineRequest mlr : req.moveLines()) {
                InventoryProduct product = productRepo.findById(mlr.productId())
                        .orElseThrow(() -> new ResourceNotFoundException("InventoryProduct", mlr.productId()));

                InventoryMoveLine line = InventoryMoveLine.builder()
                        .picking(saved)
                        .product(product)
                        .productQty(mlr.productQty() != null ? mlr.productQty() : BigDecimal.ZERO)
                        .qtyDone(BigDecimal.ZERO)
                        .location(src)
                        .locationDest(dest)
                        .build();
                moveLineRepo.save(line);
            }
        }

        log.info("Created picking: name={}, type={}", name, pt.getCode());
        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.ADMIN,
                "InventoryPicking", String.valueOf(saved.getId()), null,
                "name=" + name + ", type=" + pt.getCode() + ", lines=" + (req.moveLines() != null ? req.moveLines().size() : 0));

        return toResponse(pickingRepo.findById(saved.getId()).orElseThrow());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONFIRM — DRAFT → CONFIRMED (marks demand, optionally reserves stock)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public PickingResponse confirmPicking(Long id) {
        InventoryPicking picking = findOrThrow(id);
        if (picking.getState() != InventoryPicking.PickingState.DRAFT) {
            throw new IllegalStateException("Can only confirm a DRAFT picking. Current state: " + picking.getState());
        }

        picking.setState(InventoryPicking.PickingState.CONFIRMED);
        pickingRepo.save(picking);

        log.info("Confirmed picking: {}", picking.getName());
        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.ADMIN,
                "InventoryPicking", String.valueOf(id), "DRAFT", "CONFIRMED");

        return toResponse(picking);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VALIDATE — CONFIRMED → DONE (the actual double-entry stock move)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public PickingResponse validatePicking(Long id, List<ValidateLineRequest> lineUpdates) {
        InventoryPicking picking = findOrThrow(id);
        if (picking.getState() == InventoryPicking.PickingState.DONE
                || picking.getState() == InventoryPicking.PickingState.CANCEL) {
            throw new IllegalStateException("Cannot validate a " + picking.getState() + " picking.");
        }

        List<InventoryMoveLine> lines = moveLineRepo.findByPickingIdOrderByIdAsc(id);
        boolean hasShortfall = false;
        List<InventoryMoveLine> backorderLines = new ArrayList<>();

        for (InventoryMoveLine line : lines) {
            BigDecimal done = line.getProductQty(); // default: process full demand

            // Apply caller-supplied qtyDone overrides
            if (lineUpdates != null) {
                for (ValidateLineRequest vl : lineUpdates) {
                    if (vl.productId().equals(line.getProduct().getId()) && vl.qtyDone() != null) {
                        done = vl.qtyDone();
                        break;
                    }
                }
            }

            line.setQtyDone(done);
            moveLineRepo.save(line);

            // ── Double-entry stock move ──────────────────────────────────
            if (done.compareTo(BigDecimal.ZERO) > 0) {
                StockLedgerEntry.MovementType movType = StockLedgerEntry.MovementType.INTERNAL_TRANSFER;
                if (picking.getPickingType() != null) {
                    if (picking.getPickingType().getCode() == InventoryPickingType.PickingTypeCode.INCOMING) {
                        movType = StockLedgerEntry.MovementType.RECEIPT;
                    } else if (picking.getPickingType().getCode() == InventoryPickingType.PickingTypeCode.OUTGOING) {
                        movType = StockLedgerEntry.MovementType.DELIVERY;
                    }
                }

                adjustQuant(line.getProduct(), line.getLocation(), line.getLot(), done.negate(),
                        movType, "PICKING", picking.getName(), picking.getCreatedByUser());     // subtract from source
                adjustQuant(line.getProduct(), line.getLocationDest(), line.getLot(), done,
                        movType, "PICKING", picking.getName(), picking.getCreatedByUser());      // add to destination
            }

            // ── Shortfall → backorder ────────────────────────────────────
            BigDecimal shortfall = line.getProductQty().subtract(done);
            if (shortfall.compareTo(BigDecimal.ZERO) > 0) {
                hasShortfall = true;
                backorderLines.add(line);
            }
        }

        picking.setState(InventoryPicking.PickingState.DONE);
        picking.setDateDone(LocalDateTime.now());
        pickingRepo.save(picking);

        // Create backorder picking for shortfall lines
        if (hasShortfall) {
            createBackorder(picking, backorderLines);
        }

        log.info("Validated picking: {}, lines={}, hasBackorder={}", picking.getName(), lines.size(), hasShortfall);
        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.ADMIN,
                "InventoryPicking", String.valueOf(id),
                "state=CONFIRMED", "state=DONE, hasBackorder=" + hasShortfall);

        return toResponse(picking);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SCRAP — move stock to the scrap location
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void scrapStock(Long productId, Long locationId, BigDecimal quantity, String reason) {
        InventoryProduct product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryProduct", productId));
        InventoryLocation srcLoc = locationRepo.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryLocation", locationId));
        InventoryLocation scrapLoc = locationRepo.findByUsageAndIsActiveTrueOrderByCompleteNameAsc(
                InventoryLocation.LocationUsage.SCRAP).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No SCRAP location configured."));

        adjustQuant(product, srcLoc, null, quantity.negate(), StockLedgerEntry.MovementType.SCRAP, "SCRAP", "Reason: " + reason, null);
        adjustQuant(product, scrapLoc, null, quantity, StockLedgerEntry.MovementType.SCRAP, "SCRAP", "Reason: " + reason, null);

        log.info("Scrapped {} of product {} from {} — reason: {}", quantity, product.getName(), srcLoc.getCompleteName(), reason);
        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.ADMIN,
                "InventoryProduct", String.valueOf(productId), "qty=" + quantity,
                "SCRAPPED from " + srcLoc.getCompleteName() + ", reason=" + reason);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REPORTING
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<StockLevelReportRow> getStockLevelReport() {
        return quantRepo.findAllNonZero().stream().map(q -> {
            BigDecimal onHand = q.getQuantity();
            BigDecimal reserved = q.getReservedQty();
            BigDecimal available = onHand.subtract(reserved);
            return new StockLevelReportRow(
                    q.getProduct().getId(),
                    q.getProduct().getName(),
                    q.getProduct().getDefaultCode(),
                    onHand, reserved, available,
                    q.getLocation().getCompleteName()
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<MoveHistoryReportRow> getMoveHistoryReport() {
        // Return all validated move lines (picking state = DONE) as a flat history
        return pickingRepo.findByStateOrderByCreatedAtDesc(InventoryPicking.PickingState.DONE).stream()
                .flatMap(p -> p.getMoveLines().stream().map(ml ->
                    new MoveHistoryReportRow(
                            ml.getId(),
                            ml.getCreatedAt(),
                            p.getName(),
                            ml.getProduct().getName(),
                            ml.getLocation().getCompleteName(),
                            ml.getLocationDest().getCompleteName(),
                            ml.getQtyDone()
                    )
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockLedgerReportRow> getStockLedgerReport(Long productId, Long communityId) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 100);
        return ledgerRepo.searchLedger(communityId, productId, pageable).getContent().stream().map(e ->
            new StockLedgerReportRow(
                    e.getId(),
                    e.getProduct().getId(),
                    e.getProduct().getName(),
                    e.getProduct().getDefaultCode(),
                    e.getLocation().getId(),
                    e.getLocation().getCompleteName(),
                    e.getLot() != null ? e.getLot().getName() : null,
                    e.getMovementType() != null ? e.getMovementType().name() : null,
                    e.getReferenceType(),
                    e.getReferenceId(),
                    e.getQuantityChange(),
                    e.getResultingBalance(),
                    e.getUnitCost(),
                    e.getTotalValue(),
                    e.getCreatedAt()
            )
        ).toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryValuationReportRow> getValuationReport() {
        return quantRepo.findAllNonZero().stream().map(q -> {
            BigDecimal qty = q.getQuantity();
            BigDecimal unitCost = q.getProduct().getStandardPrice() != null ? q.getProduct().getStandardPrice() : BigDecimal.ZERO;
            BigDecimal totalVal = qty.multiply(unitCost);
            String category = q.getProduct().getCategory() != null ? q.getProduct().getCategory().getName() : "Uncategorized";
            Long whId = q.getLocation().getWarehouse() != null ? q.getLocation().getWarehouse().getId() : null;
            String whName = q.getLocation().getWarehouse() != null ? q.getLocation().getWarehouse().getName() : "Main Warehouse";
            return new InventoryValuationReportRow(
                    q.getProduct().getId(),
                    q.getProduct().getName(),
                    q.getProduct().getDefaultCode(),
                    category,
                    whId,
                    whName,
                    q.getLocation().getId(),
                    q.getLocation().getCompleteName(),
                    qty,
                    unitCost,
                    totalVal
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<StockAgingReportRow> getStockAgingReport() {
        LocalDateTime now = LocalDateTime.now();
        return quantRepo.findAllNonZero().stream().map(q -> {
            BigDecimal qty = q.getQuantity();
            BigDecimal unitCost = q.getProduct().getStandardPrice() != null ? q.getProduct().getStandardPrice() : BigDecimal.ZERO;
            BigDecimal totalVal = qty.multiply(unitCost);
            LocalDateTime movementDate = q.getUpdatedAt() != null ? q.getUpdatedAt() : LocalDateTime.now();
            long ageDays = java.time.Duration.between(movementDate, now).toDays();
            String bucket;
            if (ageDays <= 30) {
                bucket = "0-30 days";
            } else if (ageDays <= 60) {
                bucket = "31-60 days";
            } else if (ageDays <= 90) {
                bucket = "61-90 days";
            } else {
                bucket = "90+ days";
            }
            return new StockAgingReportRow(
                    q.getProduct().getId(),
                    q.getProduct().getName(),
                    q.getProduct().getDefaultCode(),
                    q.getLocation().getCompleteName(),
                    qty,
                    ageDays,
                    bucket,
                    unitCost,
                    totalVal,
                    movementDate
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<FefoRecommendationRow> getFefoRecommendations() {
        LocalDate today = LocalDate.now();
        return quantRepo.findAllNonZero().stream()
                .filter(q -> q.getLot() != null && q.getLot().getExpirationDate() != null)
                .sorted((q1, q2) -> q1.getLot().getExpirationDate().compareTo(q2.getLot().getExpirationDate()))
                .map(q -> {
                    LocalDate exp = q.getLot().getExpirationDate();
                    long days = java.time.temporal.ChronoUnit.DAYS.between(today, exp);
                    String urgency;
                    if (days <= 7) {
                        urgency = "CRITICAL";
                    } else if (days <= 30) {
                        urgency = "HIGH";
                    } else {
                        urgency = "NORMAL";
                    }
                    BigDecimal avail = q.getQuantity().subtract(q.getReservedQty() != null ? q.getReservedQty() : BigDecimal.ZERO);
                    return new FefoRecommendationRow(
                            q.getLot().getId(),
                            q.getLot().getName(),
                            q.getProduct().getId(),
                            q.getProduct().getName(),
                            q.getProduct().getDefaultCode(),
                            q.getLocation().getCompleteName(),
                            avail,
                            exp,
                            days,
                            urgency
                    );
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<ReorderAlertRow> getReorderAlerts() {
        List<InventoryProduct> products = productRepo.findAll();
        List<ReorderAlertRow> alerts = new ArrayList<>();
        BigDecimal defaultSafetyStock = BigDecimal.valueOf(10);
        BigDecimal defaultReorderQty = BigDecimal.valueOf(50);

        for (InventoryProduct prod : products) {
            BigDecimal onHand = quantRepo.findByProductId(prod.getId()).stream()
                    .map(InventoryStockQuant::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (onHand.compareTo(defaultSafetyStock) <= 0) {
                BigDecimal deficit = defaultSafetyStock.subtract(onHand);
                alerts.add(new ReorderAlertRow(
                        prod.getId(),
                        prod.getName(),
                        prod.getDefaultCode(),
                        onHand,
                        defaultSafetyStock,
                        defaultReorderQty,
                        deficit
                ));
            }
        }
        return alerts;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // INTERNALS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * The core double-entry operation: adjust a stock quant row with pessimistic row locking,
     * non-negative internal location guards, and immutable stock ledger recording.
     */
    private void adjustQuant(
            InventoryProduct product,
            InventoryLocation location,
            InventoryLot lot,
            BigDecimal delta,
            StockLedgerEntry.MovementType movementType,
            String refType,
            String refId,
            AppUser user
    ) {
        InventoryStockQuant quant;
        if (lot != null && lot.getId() != null) {
            quant = quantRepo.findForUpdateByProductIdAndLocationIdAndLotId(product.getId(), location.getId(), lot.getId())
                    .orElseGet(() -> InventoryStockQuant.builder()
                            .product(product)
                            .location(location)
                            .lot(lot)
                            .quantity(BigDecimal.ZERO)
                            .reservedQty(BigDecimal.ZERO)
                            .build());
        } else {
            quant = quantRepo.findForUpdateByProductIdAndLocationId(product.getId(), location.getId())
                    .orElseGet(() -> InventoryStockQuant.builder()
                            .product(product)
                            .location(location)
                            .quantity(BigDecimal.ZERO)
                            .reservedQty(BigDecimal.ZERO)
                            .build());
        }

        BigDecimal currentQty = quant.getQuantity() != null ? quant.getQuantity() : BigDecimal.ZERO;
        BigDecimal newQty = currentQty.add(delta);

        // Strict guard against negative physical stock for internal warehouse locations
        if (location.getUsage() == InventoryLocation.LocationUsage.INTERNAL) {
            if (newQty.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("Insufficient stock for product '" + product.getName()
                        + "' at location '" + location.getCompleteName()
                        + "'. Current on-hand: " + currentQty + ", requested deduction: " + delta.abs());
            }
            BigDecimal reserved = quant.getReservedQty() != null ? quant.getReservedQty() : BigDecimal.ZERO;
            if (newQty.compareTo(reserved) < 0) {
                throw new IllegalStateException("Stock deduction for product '" + product.getName()
                        + "' at location '" + location.getCompleteName()
                        + "' would violate reserved quantity: " + reserved);
            }
        }

        quant.setQuantity(newQty);
        quantRepo.save(quant);

        // Record immutable transaction entry in Authoritative Stock Ledger
        BigDecimal unitCost = product.getStandardPrice() != null ? product.getStandardPrice() : BigDecimal.ZERO;
        BigDecimal totalVal = delta.abs().multiply(unitCost);

        StockLedgerEntry entry = StockLedgerEntry.builder()
                .community(location.getWarehouse() != null ? location.getWarehouse().getCommunity() : null)
                .product(product)
                .location(location)
                .lot(lot)
                .movementType(movementType != null ? movementType : StockLedgerEntry.MovementType.INTERNAL_TRANSFER)
                .referenceType(refType)
                .referenceId(refId)
                .quantityChange(delta)
                .resultingBalance(newQty)
                .unitCost(unitCost)
                .totalValue(totalVal)
                .createdByUser(user)
                .build();
        ledgerRepo.save(entry);
    }

    private void adjustQuant(InventoryProduct product, InventoryLocation location, BigDecimal delta) {
        adjustQuant(product, location, null, delta, StockLedgerEntry.MovementType.INTERNAL_TRANSFER, "MANUAL", null, null);
    }

    /**
     * Creates a backorder picking for shortfall lines from a partially-validated picking.
     */
    private void createBackorder(InventoryPicking original, List<InventoryMoveLine> shortfallLines) {
        String boName = nextSequence(original.getPickingType().getSequencePrefix());

        InventoryPicking backorder = InventoryPicking.builder()
                .name(boName)
                .state(InventoryPicking.PickingState.CONFIRMED)
                .pickingType(original.getPickingType())
                .location(original.getLocation())
                .locationDest(original.getLocationDest())
                .scheduledDate(original.getScheduledDate())
                .origin("Backorder of " + original.getName())
                .backorder(original)
                .community(original.getCommunity())
                .createdByUser(original.getCreatedByUser())
                .build();

        InventoryPicking savedBo = pickingRepo.save(backorder);

        for (InventoryMoveLine origLine : shortfallLines) {
            BigDecimal remaining = origLine.getProductQty().subtract(origLine.getQtyDone());
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                InventoryMoveLine boLine = InventoryMoveLine.builder()
                        .picking(savedBo)
                        .product(origLine.getProduct())
                        .productQty(remaining)
                        .qtyDone(BigDecimal.ZERO)
                        .location(origLine.getLocation())
                        .locationDest(origLine.getLocationDest())
                        .build();
                moveLineRepo.save(boLine);
            }
        }

        log.info("Created backorder {} for {}, shortfall lines={}", boName, original.getName(), shortfallLines.size());
    }

    /** Atomically generate the next sequence number for a picking type prefix. */
    private String nextSequence(String prefix) {
        InventorySequence seq = seqRepo.findByPrefixForUpdate(prefix)
                .orElseGet(() -> {
                    InventorySequence s = InventorySequence.builder().prefix(prefix).nextVal(1L).build();
                    return seqRepo.save(s);
                });

        String name = prefix + String.format("%05d", seq.getNextVal());
        seq.setNextVal(seq.getNextVal() + 1);
        seqRepo.save(seq);
        return name;
    }

    private InventoryPicking findOrThrow(Long id) {
        return pickingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryPicking", id));
    }

    private PickingResponse toResponse(InventoryPicking p) {
        List<InventoryMoveLine> lines = moveLineRepo.findByPickingIdOrderByIdAsc(p.getId());
        List<MoveLineResponse> lineDtos = lines.stream().map(ml ->
            new MoveLineResponse(
                    ml.getId(),
                    ml.getProduct().getId(),
                    ml.getProduct().getName(),
                    ml.getProductQty(),
                    ml.getQtyDone()
            )
        ).toList();

        return new PickingResponse(
                p.getId(), p.getName(), p.getState().name(),
                p.getPartnerId(),
                p.getPickingType().getId(),
                p.getLocation().getId(),
                p.getLocationDest().getId(),
                p.getScheduledDate(),
                p.getOrigin(),
                lineDtos
        );
    }
}
