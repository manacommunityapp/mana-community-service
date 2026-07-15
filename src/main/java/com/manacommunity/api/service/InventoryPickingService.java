package com.manacommunity.api.service;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
                adjustQuant(line.getProduct(), line.getLocation(), done.negate());     // subtract from source
                adjustQuant(line.getProduct(), line.getLocationDest(), done);           // add to destination
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

        adjustQuant(product, srcLoc, quantity.negate());
        adjustQuant(product, scrapLoc, quantity);

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

    // ═══════════════════════════════════════════════════════════════════════
    // INTERNALS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * The core double-entry operation: adjust a stock quant row.
     * Creates the quant if it doesn't exist; updates atomically if it does.
     */
    private void adjustQuant(InventoryProduct product, InventoryLocation location, BigDecimal delta) {
        InventoryStockQuant quant = quantRepo.findByProductIdAndLocationId(product.getId(), location.getId())
                .orElseGet(() -> InventoryStockQuant.builder()
                        .product(product)
                        .location(location)
                        .quantity(BigDecimal.ZERO)
                        .reservedQty(BigDecimal.ZERO)
                        .build());

        quant.setQuantity(quant.getQuantity().add(delta));
        quantRepo.save(quant);
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
