package com.manacommunity.api.freshkitchen.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.freshkitchen.dto.*;
import com.manacommunity.api.freshkitchen.model.*;
import com.manacommunity.api.freshkitchen.repository.BreakfastComboItemRepository;
import com.manacommunity.api.freshkitchen.repository.BreakfastComboRepository;
import com.manacommunity.api.freshkitchen.repository.FoodProductionBatchRepository;
import com.manacommunity.api.freshkitchen.repository.FreshFoodProductConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreshKitchenService {

    private final FreshFoodProductConfigRepository productConfigRepository;
    private final BreakfastComboRepository comboRepository;
    private final BreakfastComboItemRepository comboItemRepository;
    private final FoodProductionBatchRepository batchRepository;

    @Transactional(readOnly = true)
    public List<FreshProductDto> getAllProducts() {
        return productConfigRepository.findByIsActiveTrue()
                .stream()
                .map(FreshProductDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FreshProductDto> getProductsByCategory(FoodCategory category) {
        return productConfigRepository.findByCategoryAndIsActiveTrue(category)
                .stream()
                .map(FreshProductDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FreshProductDto> getBatters() {
        return getProductsByCategory(FoodCategory.BATTER);
    }

    @Transactional(readOnly = true)
    public FreshProductDto getProductByProductId(String productId) {
        FreshFoodProductConfig config = productConfigRepository.findByProductIdAndIsActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("FreshFoodProductConfig", productId));
        return FreshProductDto.fromEntity(config);
    }

    @Transactional(readOnly = true)
    public List<BreakfastComboDto> getAllBreakfastCombos() {
        return comboRepository.findByIsActiveTrue()
                .stream()
                .map(BreakfastComboDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public BreakfastComboDto getBreakfastComboByCode(String comboCode) {
        BreakfastCombo combo = comboRepository.findByComboCodeAndIsActiveTrue(comboCode)
                .orElseThrow(() -> new ResourceNotFoundException("BreakfastCombo", comboCode));
        return BreakfastComboDto.fromEntity(combo);
    }

    /**
     * Calculates tailored breakfast combo price and item breakdown with Bill of Materials (BOM) validation.
     */
    @Transactional(readOnly = true)
    public CustomizedComboResponseDto customizeCombo(CustomizeComboRequest request) {
        BreakfastCombo combo = comboRepository.findByComboCodeAndIsActiveTrue(request.getComboCode())
                .orElseThrow(() -> new ResourceNotFoundException("BreakfastCombo", request.getComboCode()));

        BigDecimal basePrice = combo.getDiscountedPrice() != null ? combo.getDiscountedPrice() : combo.getBasePrice();
        BigDecimal priceAdjustment = BigDecimal.ZERO;
        List<CustomizedComboResponseDto.CustomizedItemDetail> details = new ArrayList<>();

        Map<String, BreakfastComboItem> bomMap = combo.getItems().stream()
                .collect(Collectors.toMap(BreakfastComboItem::getProductId, item -> item));

        Set<String> processedBoms = new HashSet<>();

        if (request.getSelectedItems() != null) {
            for (CustomizeComboRequest.CustomizedItemSelection sel : request.getSelectedItems()) {
                BreakfastComboItem originalBom = bomMap.get(sel.getOriginalProductId());
                if (originalBom == null) {
                    throw new InvalidInputException("Item " + sel.getOriginalProductId() + " is not part of combo " + combo.getComboCode());
                }

                processedBoms.add(originalBom.getProductId());
                int qty = sel.getQuantity() != null && sel.getQuantity() > 0 ? sel.getQuantity() : originalBom.getDefaultQuantity();

                String chosenProductId = sel.getSelectedProductId() != null ? sel.getSelectedProductId() : originalBom.getProductId();

                // Validate substitution
                if (!chosenProductId.equals(originalBom.getProductId())) {
                    if (!Boolean.TRUE.equals(originalBom.getIsCustomizable())) {
                        throw new InvalidInputException("Item " + originalBom.getProductName() + " cannot be substituted");
                    }
                    if (originalBom.getAllowedSubstitutes() != null) {
                        List<String> allowed = Arrays.stream(originalBom.getAllowedSubstitutes().split(","))
                                .map(String::trim)
                                .toList();
                        if (!allowed.contains(chosenProductId)) {
                            throw new InvalidInputException("Product " + chosenProductId + " is not an allowed substitute for " + originalBom.getProductId());
                        }
                    }
                }

                FreshFoodProductConfig chosenConfig = productConfigRepository.findByProductId(chosenProductId)
                        .orElse(null);

                String chosenName = chosenConfig != null ? chosenConfig.getName() : originalBom.getProductName();
                BigDecimal chosenUnitPrice = chosenConfig != null ? chosenConfig.getUnitPrice() : originalBom.getUnitPrice();

                // Price adjustment = (chosenPrice * qty) - (originalBomPrice * originalBomQty)
                BigDecimal originalSubtotal = originalBom.getUnitPrice().multiply(BigDecimal.valueOf(originalBom.getDefaultQuantity()));
                BigDecimal newSubtotal = chosenUnitPrice.multiply(BigDecimal.valueOf(qty));
                BigDecimal diff = newSubtotal.subtract(originalSubtotal);
                priceAdjustment = priceAdjustment.add(diff);

                details.add(CustomizedComboResponseDto.CustomizedItemDetail.builder()
                        .originalProductId(originalBom.getProductId())
                        .selectedProductId(chosenProductId)
                        .selectedProductName(chosenName)
                        .quantity(qty)
                        .unitPrice(chosenUnitPrice)
                        .subtotal(newSubtotal)
                        .itemRole(originalBom.getItemRole() != null ? originalBom.getItemRole().name() : "MAIN")
                        .build());
            }
        }

        // Include any mandatory BOM items that were not customized
        for (BreakfastComboItem bom : combo.getItems()) {
            if (!processedBoms.contains(bom.getProductId())) {
                BigDecimal sub = bom.getUnitPrice().multiply(BigDecimal.valueOf(bom.getDefaultQuantity()));
                details.add(CustomizedComboResponseDto.CustomizedItemDetail.builder()
                        .originalProductId(bom.getProductId())
                        .selectedProductId(bom.getProductId())
                        .selectedProductName(bom.getProductName())
                        .quantity(bom.getDefaultQuantity())
                        .unitPrice(bom.getUnitPrice())
                        .subtotal(sub)
                        .itemRole(bom.getItemRole() != null ? bom.getItemRole().name() : "MAIN")
                        .build());
            }
        }

        BigDecimal finalPrice = basePrice.add(priceAdjustment).max(BigDecimal.ZERO);

        return CustomizedComboResponseDto.builder()
                .comboCode(combo.getComboCode())
                .comboName(combo.getName())
                .basePrice(basePrice)
                .priceAdjustment(priceAdjustment)
                .finalPrice(finalPrice)
                .itemBreakdown(details)
                .build();
    }

    /**
     * Create a new food production batch.
     */
    @Transactional
    public ProductionBatchDto createProductionBatch(ProductionBatchDto dto) {
        if (dto.getProductId() == null || dto.getVendorId() == null) {
            throw new InvalidInputException("productId and vendorId are required");
        }

        FreshFoodProductConfig config = productConfigRepository.findByProductId(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("FreshFoodProductConfig", dto.getProductId()));

        String batchNumber = dto.getBatchNumber();
        if (batchNumber == null || batchNumber.isBlank()) {
            String dateStr = LocalDate.now().toString().replace("-", "");
            batchNumber = "BATCH-" + dto.getProductId() + "-" + dateStr + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }

        int shelfLifeHours = config.getShelfLifeHours() != null ? config.getShelfLifeHours() : 48;
        LocalDateTime expiry = dto.getExpiryDateTime() != null ? dto.getExpiryDateTime() : LocalDateTime.now().plusHours(shelfLifeHours);

        FoodProductionBatch batch = FoodProductionBatch.builder()
                .batchNumber(batchNumber)
                .productId(dto.getProductId())
                .productName(config.getName())
                .vendorId(dto.getVendorId())
                .productionDate(dto.getProductionDate() != null ? dto.getProductionDate() : LocalDate.now())
                .batchQuantity(dto.getBatchQuantity() != null ? dto.getBatchQuantity() : 0)
                .storageTempCelsius(dto.getStorageTempCelsius())
                .batchStatus(BatchStatus.PLANNED)
                .expiryDateTime(expiry)
                .notes(dto.getNotes())
                .build();

        batch = batchRepository.save(batch);
        return ProductionBatchDto.fromEntity(batch);
    }

    /**
     * Cold-Chain Quality Gate Release:
     * Batters and Fresh Cuts MUST be stored between 2.0°C and 4.0°C.
     * Batches outside cold chain safety range will be flagged or rejected.
     */
    @Transactional
    public ProductionBatchDto releaseBatch(String batchNumber, BatchReleaseRequest request) {
        FoodProductionBatch batch = batchRepository.findByBatchNumber(batchNumber)
                .orElseThrow(() -> new ResourceNotFoundException("FoodProductionBatch", batchNumber));

        FreshFoodProductConfig config = productConfigRepository.findByProductId(batch.getProductId())
                .orElse(null);

        BigDecimal minTemp = config != null && config.getStorageTempMin() != null ? config.getStorageTempMin() : new BigDecimal("2.0");
        BigDecimal maxTemp = config != null && config.getStorageTempMax() != null ? config.getStorageTempMax() : new BigDecimal("4.0");

        BigDecimal temp = request.getStorageTempCelsius();
        if (temp == null) {
            throw new InvalidInputException("Storage temperature reading is strictly required for fresh food release gate");
        }

        batch.setStorageTempCelsius(temp);
        batch.setReleasedBy(request.getReleasedBy() != null ? request.getReleasedBy() : "QA_OPERATIONS");
        batch.setReleasedAt(LocalDateTime.now());
        if (request.getNotes() != null) {
            batch.setNotes((batch.getNotes() != null ? batch.getNotes() + " | " : "") + request.getNotes());
        }

        if (temp.compareTo(minTemp) < 0 || temp.compareTo(maxTemp) > 0) {
            log.warn("Batch {} failed cold-chain quality check: temp {}°C outside permitted range [{}°C - {}°C]",
                    batchNumber, temp, minTemp, maxTemp);
            batch.setBatchStatus(BatchStatus.REJECTED);
            batch = batchRepository.save(batch);
            throw new InvalidInputException("Batch cold-chain temperature " + temp + "°C violates safety bounds ("
                    + minTemp + "°C to " + maxTemp + "°C). Batch marked as REJECTED.");
        }

        batch.setBatchStatus(BatchStatus.RELEASED);
        batch = batchRepository.save(batch);
        log.info("Batch {} successfully passed cold chain quality check at {}°C and released", batchNumber, temp);
        return ProductionBatchDto.fromEntity(batch);
    }

    @Transactional(readOnly = true)
    public List<ProductionBatchDto> getBatchesByVendor(String vendorId, LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return batchRepository.findByVendorIdAndProductionDate(vendorId, queryDate)
                .stream()
                .map(ProductionBatchDto::fromEntity)
                .toList();
    }

    /**
     * Automated Expiry Lockout:
     * Scans active released batches where use-by timestamp is past, marks them EXPIRED.
     */
    @Transactional
    public int expireOverdueBatches() {
        LocalDateTime now = LocalDateTime.now();
        List<FoodProductionBatch> overdue = batchRepository.findByBatchStatusAndExpiryDateTimeBefore(BatchStatus.RELEASED, now);
        for (FoodProductionBatch batch : overdue) {
            batch.setBatchStatus(BatchStatus.EXPIRED);
            log.info("Batch {} automatically locked out due to expiry at {}", batch.getBatchNumber(), batch.getExpiryDateTime());
        }
        if (!overdue.isEmpty()) {
            batchRepository.saveAll(overdue);
        }
        return overdue.size();
    }
}
