package com.manacommunity.api.fresh.inventory.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.fresh.city.model.FreshFulfilmentLocation;
import com.manacommunity.api.fresh.city.repository.FreshFulfilmentLocationRepository;
import com.manacommunity.api.fresh.inventory.dto.*;
import com.manacommunity.api.fresh.inventory.model.*;
import com.manacommunity.api.fresh.inventory.repository.FreshProductWastageRepository;
import com.manacommunity.api.fresh.inventory.repository.FreshStockMovementRepository;
import com.manacommunity.api.freshkitchen.model.FoodProductionBatch;
import com.manacommunity.api.freshkitchen.repository.FoodProductionBatchRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.vendor.commerce.repository.VendorProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreshStockLedgerService {

    private final FreshStockMovementRepository movementRepository;
    private final FreshProductWastageRepository wastageRepository;
    private final VendorProductRepository vendorProductRepository;
    private final FoodProductionBatchRepository batchRepository;
    private final FreshFulfilmentLocationRepository locationRepository;
    private final AppUserRepository userRepository;

    @Transactional
    public StockMovementDto recordStockMovement(RecordStockMovementRequest req) {
        VendorProduct product = vendorProductRepository.findById(req.getVendorProductId())
                .orElseThrow(() -> new ResourceNotFoundException("VendorProduct", req.getVendorProductId()));

        FoodProductionBatch batch = req.getBatchId() != null
                ? batchRepository.findById(req.getBatchId()).orElse(null)
                : null;

        FreshFulfilmentLocation location = req.getFulfilmentLocationId() != null
                ? locationRepository.findById(req.getFulfilmentLocationId()).orElse(null)
                : null;

        AppUser createdBy = req.getCreatedByUserId() != null
                ? userRepository.findById(req.getCreatedByUserId()).orElse(null)
                : null;

        Optional<FreshStockMovement> lastMovement = movementRepository
                .findFirstByVendorProductIdOrderByCreatedAtDesc(product.getId());

        BigDecimal currentBalance = lastMovement.map(FreshStockMovement::getBalanceAfter).orElse(BigDecimal.ZERO);
        BigDecimal qty = req.getQuantity();
        BigDecimal newBalance;

        switch (req.getMovementType()) {
            case INWARD_RECEIPT:
            case ORDER_CANCEL_RESTOCK:
                newBalance = currentBalance.add(qty.abs());
                break;
            case ORDER_PICK:
            case ORDER_RESERVE:
            case WASTAGE_WRITE_OFF:
            case RECALL_QUARANTINE:
                newBalance = currentBalance.subtract(qty.abs());
                break;
            case AUDIT_ADJUSTMENT:
            default:
                newBalance = currentBalance.add(qty);
                break;
        }

        FreshStockMovement movement = FreshStockMovement.builder()
                .vendorProduct(product)
                .batch(batch)
                .fulfilmentLocation(location)
                .movementType(req.getMovementType())
                .quantity(qty)
                .balanceAfter(newBalance)
                .unitOfMeasure(req.getUnitOfMeasure() != null ? req.getUnitOfMeasure() : "KG")
                .referenceType(req.getReferenceType())
                .referenceId(req.getReferenceId())
                .createdByUser(createdBy)
                .notes(req.getNotes())
                .build();

        FreshStockMovement saved = movementRepository.save(movement);
        log.info("Recorded stock movement for product {}: type={}, qty={}, balance={}",
                product.getName(), req.getMovementType(), qty, newBalance);

        return toMovementDto(saved);
    }

    @Transactional
    public ProductWastageDto recordWastage(RecordWastageRequest req) {
        VendorProduct product = vendorProductRepository.findById(req.getVendorProductId())
                .orElseThrow(() -> new ResourceNotFoundException("VendorProduct", req.getVendorProductId()));

        FoodProductionBatch batch = req.getBatchId() != null
                ? batchRepository.findById(req.getBatchId()).orElse(null)
                : null;

        FreshFulfilmentLocation location = req.getFulfilmentLocationId() != null
                ? locationRepository.findById(req.getFulfilmentLocationId()).orElse(null)
                : null;

        AppUser recordedBy = userRepository.findById(req.getRecordedByUserId())
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", req.getRecordedByUserId()));

        BigDecimal costPrice = req.getCostPricePerUnit() != null
                ? req.getCostPricePerUnit()
                : BigDecimal.ZERO;

        BigDecimal totalLoss = costPrice.multiply(req.getQuantity()).setScale(2, RoundingMode.HALF_UP);

        String ref = "WST-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))
                + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();

        FreshProductWastage wastage = FreshProductWastage.builder()
                .wastageReference(ref)
                .vendorProduct(product)
                .batch(batch)
                .fulfilmentLocation(location)
                .quantity(req.getQuantity())
                .unitOfMeasure(req.getUnitOfMeasure() != null ? req.getUnitOfMeasure() : "KG")
                .costPricePerUnit(costPrice)
                .totalLossAmount(totalLoss)
                .wastageCategory(req.getWastageCategory())
                .notes(req.getNotes())
                .disposalMethod(req.getDisposalMethod())
                .recordedByUser(recordedBy)
                .build();

        FreshProductWastage savedWastage = wastageRepository.save(wastage);

        // Deduct from stock movement ledger automatically
        recordStockMovement(RecordStockMovementRequest.builder()
                .vendorProductId(product.getId())
                .batchId(batch != null ? batch.getId() : null)
                .fulfilmentLocationId(location != null ? location.getId() : null)
                .movementType(StockMovementType.WASTAGE_WRITE_OFF)
                .quantity(req.getQuantity().negate())
                .unitOfMeasure(wastage.getUnitOfMeasure())
                .referenceType("WASTAGE_RECORD")
                .referenceId(ref)
                .createdByUserId(recordedBy.getId())
                .notes("Automated write-off from wastage report: " + req.getWastageCategory())
                .build());

        log.info("Recorded wastage {} for product {}: qty={}, loss=₹{}, category={}",
                ref, product.getName(), req.getQuantity(), totalLoss, req.getWastageCategory());

        return toWastageDto(savedWastage);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getMovementsByProduct(Long productId) {
        return movementRepository.findByVendorProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::toMovementDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getMovementsByBatch(Long batchId) {
        return movementRepository.findByBatchIdOrderByCreatedAtDesc(batchId).stream()
                .map(this::toMovementDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductWastageDto> getAllWastages() {
        return wastageRepository.findAll().stream()
                .map(this::toWastageDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductWastageDto> getWastagesByProduct(Long productId) {
        return wastageRepository.findByVendorProductId(productId).stream()
                .map(this::toWastageDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductWastageDto> getWastagesByCategory(WastageCategory category) {
        return wastageRepository.findByWastageCategory(category).stream()
                .map(this::toWastageDto)
                .collect(Collectors.toList());
    }

    public StockMovementDto toMovementDto(FreshStockMovement m) {
        return StockMovementDto.builder()
                .id(m.getId())
                .vendorProductId(m.getVendorProduct().getId())
                .productName(m.getVendorProduct().getName())
                .batchId(m.getBatch() != null ? m.getBatch().getId() : null)
                .batchNumber(m.getBatch() != null ? m.getBatch().getBatchNumber() : null)
                .fulfilmentLocationId(m.getFulfilmentLocation() != null ? m.getFulfilmentLocation().getId() : null)
                .fulfilmentLocationName(m.getFulfilmentLocation() != null ? m.getFulfilmentLocation().getName() : null)
                .movementType(m.getMovementType())
                .quantity(m.getQuantity())
                .balanceAfter(m.getBalanceAfter())
                .unitOfMeasure(m.getUnitOfMeasure())
                .referenceType(m.getReferenceType())
                .referenceId(m.getReferenceId())
                .createdByUserId(m.getCreatedByUser() != null ? m.getCreatedByUser().getId() : null)
                .notes(m.getNotes())
                .createdAt(m.getCreatedAt())
                .build();
    }

    public ProductWastageDto toWastageDto(FreshProductWastage w) {
        return ProductWastageDto.builder()
                .id(w.getId())
                .wastageReference(w.getWastageReference())
                .vendorProductId(w.getVendorProduct().getId())
                .productName(w.getVendorProduct().getName())
                .batchId(w.getBatch() != null ? w.getBatch().getId() : null)
                .batchNumber(w.getBatch() != null ? w.getBatch().getBatchNumber() : null)
                .fulfilmentLocationId(w.getFulfilmentLocation() != null ? w.getFulfilmentLocation().getId() : null)
                .fulfilmentLocationName(w.getFulfilmentLocation() != null ? w.getFulfilmentLocation().getName() : null)
                .quantity(w.getQuantity())
                .unitOfMeasure(w.getUnitOfMeasure())
                .costPricePerUnit(w.getCostPricePerUnit())
                .totalLossAmount(w.getTotalLossAmount())
                .wastageCategory(w.getWastageCategory())
                .notes(w.getNotes())
                .disposalMethod(w.getDisposalMethod())
                .recordedByUserId(w.getRecordedByUser().getId())
                .recordedByUserName(w.getRecordedByUser().getFullName())
                .recordedAt(w.getRecordedAt())
                .build();
    }
}
