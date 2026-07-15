package com.manacommunity.api.service;

import com.manacommunity.api.dto.InventoryScrapRequest;
import com.manacommunity.api.dto.InventoryScrapResponse;
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
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryScrapService {

    private final InventoryScrapRepository scrapRepo;
    private final InventoryProductRepository productRepo;
    private final InventoryLocationRepository locationRepo;
    private final InventoryLotRepository lotRepo;
    private final InventoryStockQuantRepository quantRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<InventoryScrapResponse> getAll() {
        return scrapRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryScrapResponse> getByProduct(Long productId) {
        return scrapRepo.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Creates a scrap record in DRAFT state. Call {@link #confirmScrap(Long, Long)}
     * to actually move the stock to the scrap location.
     */
    @Transactional
    public InventoryScrapResponse createScrap(InventoryScrapRequest req) {
        InventoryProduct product = productRepo.findById(req.productId())
                .orElseThrow(() -> new ResourceNotFoundException("InventoryProduct", req.productId()));
        InventoryLocation srcLoc = locationRepo.findById(req.locationId())
                .orElseThrow(() -> new ResourceNotFoundException("InventoryLocation", req.locationId()));
        InventoryLocation scrapLoc = locationRepo
                .findByUsageAndIsActiveTrueOrderByCompleteNameAsc(InventoryLocation.LocationUsage.SCRAP)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No SCRAP location configured."));

        if (req.quantity() == null || req.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Scrap quantity must be positive.");
        }

        // Validate lot if product is tracked
        InventoryLot lot = null;
        if (req.lotId() != null) {
            lot = lotRepo.findById(req.lotId())
                    .orElseThrow(() -> new ResourceNotFoundException("InventoryLot", req.lotId()));
            if (!lot.getProduct().getId().equals(product.getId())) {
                throw new IllegalArgumentException("Lot " + lot.getName() + " does not belong to product " + product.getName());
            }
        } else if (product.getTracking() != InventoryProduct.TrackingType.NONE) {
            throw new IllegalArgumentException(
                    "Product '" + product.getName() + "' requires lot/serial tracking. Provide a lotId.");
        }

        // Validate sufficient stock
        BigDecimal onHand = getOnHand(product.getId(), srcLoc.getId(), lot != null ? lot.getId() : null);
        if (onHand.compareTo(req.quantity()) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient stock. On hand: " + onHand + ", requested scrap: " + req.quantity());
        }

        InventoryScrap scrap = InventoryScrap.builder()
                .product(product)
                .lot(lot)
                .sourceLocation(srcLoc)
                .scrapLocation(scrapLoc)
                .quantity(req.quantity())
                .reason(req.reason())
                .state(InventoryScrap.ScrapState.DRAFT)
                .build();

        InventoryScrap saved = scrapRepo.save(scrap);
        log.info("Created scrap record: id={}, product={}, qty={}", saved.getId(), product.getName(), req.quantity());

        return toResponse(saved);
    }

    /**
     * Confirms a DRAFT scrap — actually moves the stock to the scrap location.
     */
    @Transactional
    public InventoryScrapResponse confirmScrap(Long scrapId, Long userId) {
        InventoryScrap scrap = scrapRepo.findById(scrapId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryScrap", scrapId));

        if (scrap.getState() != InventoryScrap.ScrapState.DRAFT) {
            throw new IllegalStateException("Can only confirm a DRAFT scrap. Current: " + scrap.getState());
        }

        // Double-entry: subtract from source, add to scrap location
        adjustQuant(scrap.getProduct(), scrap.getSourceLocation(),
                scrap.getLot(), scrap.getQuantity().negate());
        adjustQuant(scrap.getProduct(), scrap.getScrapLocation(),
                scrap.getLot(), scrap.getQuantity());

        scrap.setState(InventoryScrap.ScrapState.DONE);
        scrap.setScrappedAt(LocalDateTime.now());
        if (userId != null) {
            scrap.setScrappedByUser(AppUser.builder().id(userId).build());
        }
        scrapRepo.save(scrap);

        auditService.record(AuditAction.PRODUCT_DELETED, AuditModule.ADMIN,
                "InventoryScrap", String.valueOf(scrapId), null,
                "product=" + scrap.getProduct().getName()
                        + ", qty=" + scrap.getQuantity()
                        + ", from=" + scrap.getSourceLocation().getCompleteName()
                        + ", reason=" + scrap.getReason());

        log.info("Confirmed scrap: id={}, product={}, qty={}", scrapId, scrap.getProduct().getName(), scrap.getQuantity());
        return toResponse(scrap);
    }

    /**
     * Quick scrap — create + confirm in one call (matches the existing frontend flow
     * which doesn't have a two-step draft→confirm UX for scrap).
     */
    @Transactional
    public InventoryScrapResponse quickScrap(InventoryScrapRequest req, Long userId) {
        InventoryScrapResponse draft = createScrap(req);
        return confirmScrap(draft.id(), userId);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private BigDecimal getOnHand(Long productId, Long locationId, Long lotId) {
        return quantRepo.findByProductIdAndLocationIdAndLotId(productId, locationId, lotId)
                .map(InventoryStockQuant::getQuantity)
                .orElse(BigDecimal.ZERO);
    }

    private void adjustQuant(InventoryProduct product, InventoryLocation location,
                             InventoryLot lot, BigDecimal delta) {
        Long lotId = lot != null ? lot.getId() : null;
        InventoryStockQuant quant = quantRepo.findByProductIdAndLocationIdAndLotId(product.getId(), location.getId(), lotId)
                .orElseGet(() -> InventoryStockQuant.builder()
                        .product(product)
                        .location(location)
                        .lot(lot)
                        .quantity(BigDecimal.ZERO)
                        .reservedQty(BigDecimal.ZERO)
                        .build());

        quant.setQuantity(quant.getQuantity().add(delta));
        quantRepo.save(quant);
    }

    private InventoryScrapResponse toResponse(InventoryScrap s) {
        return new InventoryScrapResponse(
                s.getId(),
                s.getProduct().getId(),
                s.getProduct().getName(),
                s.getLot() != null ? s.getLot().getId() : null,
                s.getLot() != null ? s.getLot().getName() : null,
                s.getSourceLocation().getCompleteName(),
                s.getScrapLocation().getCompleteName(),
                s.getQuantity(),
                s.getReason(),
                s.getState().name(),
                s.getScrappedByUser() != null ? s.getScrappedByUser().getFullName() : null,
                s.getScrappedAt(),
                s.getCreatedAt()
        );
    }
}
