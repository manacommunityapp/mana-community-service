package com.manacommunity.api.fresh.processing.service;

import com.manacommunity.api.fresh.processing.dto.*;
import com.manacommunity.api.fresh.processing.model.*;
import com.manacommunity.api.fresh.processing.repository.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.vendor.commerce.repository.VendorProductRepository;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FreshProcessingService {

    private final FreshProcessingRecipeRepository recipeRepository;
    private final FreshProcessingBatchRepository batchRepository;
    private final FreshBatchQualityCheckRepository qualityCheckRepository;
    private final FreshBatchReleaseRepository batchReleaseRepository;
    private final FreshBatchTemperatureLogRepository temperatureLogRepository;
    private final VmsVendorRepository vendorRepository;
    private final VendorProductRepository vendorProductRepository;
    private final AppUserRepository userRepository;

    // ----- Recipes -----

    @Transactional
    public RecipeDto createRecipe(CreateRecipeRequest req) {
        VmsVendor vendor = vendorRepository.findById(req.getVendorId())
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + req.getVendorId()));
        VendorProduct product = vendorProductRepository.findById(req.getOutputProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + req.getOutputProductId()));

        FreshProcessingRecipe recipe = FreshProcessingRecipe.builder()
                .vendor(vendor)
                .recipeCode(req.getRecipeCode())
                .recipeName(req.getRecipeName())
                .outputProduct(product)
                .targetOutputUnit(req.getTargetOutputUnit())
                .expectedYieldPercent(req.getExpectedYieldPercent() != null ? req.getExpectedYieldPercent() : BigDecimal.valueOf(100.00))
                .storageTempMinCelsius(req.getStorageTempMinCelsius() != null ? req.getStorageTempMinCelsius() : BigDecimal.valueOf(2.0))
                .storageTempMaxCelsius(req.getStorageTempMaxCelsius() != null ? req.getStorageTempMaxCelsius() : BigDecimal.valueOf(4.0))
                .shelfLifeHours(req.getShelfLifeHours() != null ? req.getShelfLifeHours() : 24)
                .instructions(req.getInstructions())
                .build();

        recipeRepository.save(recipe);
        return toRecipeDto(recipe);
    }

    public List<RecipeDto> getRecipesByVendor(Long vendorId) {
        return recipeRepository.findByVendorId(vendorId).stream()
                .map(this::toRecipeDto)
                .collect(Collectors.toList());
    }

    // ----- Batches -----

    @Transactional
    public BatchDto createBatch(CreateBatchRequest req) {
        VmsVendor vendor = vendorRepository.findById(req.getVendorId())
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
        VendorProduct product = vendorProductRepository.findById(req.getVendorProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        FreshProcessingRecipe recipe = null;
        if (req.getRecipeId() != null) {
            recipe = recipeRepository.findById(req.getRecipeId()).orElse(null);
        }

        String batchNumber = "BATCH-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Instant expiry = req.getExpiryTimestamp();
        if (expiry == null) {
            int shelfLife = (recipe != null && recipe.getShelfLifeHours() != null) ? recipe.getShelfLifeHours() : 24;
            expiry = Instant.now().plusSeconds((long) shelfLife * 3600);
        }

        FreshProcessingBatch batch = FreshProcessingBatch.builder()
                .batchNumber(batchNumber)
                .recipe(recipe)
                .vendor(vendor)
                .vendorProduct(product)
                .productionDate(req.getProductionDate() != null ? req.getProductionDate() : LocalDate.now())
                .rawMaterialInputQty(req.getRawMaterialInputQty())
                .actualOutputQty(BigDecimal.ZERO)
                .actualYieldPercent(BigDecimal.ZERO)
                .wastageQty(BigDecimal.ZERO)
                .status(BatchStatus.IN_PRODUCTION)
                .storageTempCelsius(req.getStorageTempCelsius())
                .expiryTimestamp(expiry)
                .notes(req.getNotes())
                .build();

        batchRepository.save(batch);
        return toBatchDto(batch);
    }

    @Transactional
    public BatchDto recordOutput(Long batchId, RecordOutputRequest req) {
        FreshProcessingBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        batch.setActualOutputQty(req.getActualOutputQty());
        if (req.getWastageQty() != null) {
            batch.setWastageQty(req.getWastageQty());
        }
        if (req.getWastageReason() != null) {
            batch.setWastageReason(req.getWastageReason());
        }
        if (req.getStorageTempCelsius() != null) {
            batch.setStorageTempCelsius(req.getStorageTempCelsius());
        }

        if (batch.getRawMaterialInputQty() != null && batch.getRawMaterialInputQty().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal yieldPct = req.getActualOutputQty()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(batch.getRawMaterialInputQty(), 2, RoundingMode.HALF_UP);
            batch.setActualYieldPercent(yieldPct);
        }

        batch.setStatus(BatchStatus.QUALITY_INSPECTION);
        batchRepository.save(batch);
        return toBatchDto(batch);
    }

    @Transactional
    public QualityCheckDto addQualityCheck(Long batchId, AddQualityCheckRequest req) {
        FreshProcessingBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));
        AppUser user = userRepository.findById(req.getCheckedByUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + req.getCheckedByUserId()));

        FreshBatchQualityCheck qc = FreshBatchQualityCheck.builder()
                .batch(batch)
                .checkName(req.getCheckName())
                .measuredValue(req.getMeasuredValue())
                .isPassed(Boolean.TRUE.equals(req.getIsPassed()))
                .checkedByUser(user)
                .notes(req.getNotes())
                .build();

        qualityCheckRepository.save(qc);

        // If any check fails, flag batch as QUARANTINED
        if (!Boolean.TRUE.equals(req.getIsPassed())) {
            batch.setStatus(BatchStatus.QUARANTINED);
            batchRepository.save(batch);
        }

        return toQualityCheckDto(qc);
    }

    @Transactional
    public BatchReleaseDto releaseBatch(Long batchId, BatchReleaseRequest req) {
        FreshProcessingBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));
        AppUser user = userRepository.findById(req.getReleasedByUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + req.getReleasedByUserId()));

        FreshBatchRelease release = FreshBatchRelease.builder()
                .batch(batch)
                .releasedByUser(user)
                .releaseDecision(req.getReleaseDecision())
                .temperatureVerifiedCelsius(req.getTemperatureVerifiedCelsius())
                .remarks(req.getRemarks())
                .build();

        batchReleaseRepository.save(release);

        // Update batch status based on decision
        if (req.getReleaseDecision() == ReleaseDecision.APPROVED) {
            batch.setStatus(BatchStatus.RELEASED);
        } else if (req.getReleaseDecision() == ReleaseDecision.REJECTED) {
            batch.setStatus(BatchStatus.REJECTED);
        } else {
            batch.setStatus(BatchStatus.QUARANTINED);
        }
        batchRepository.save(batch);

        return toBatchReleaseDto(release);
    }

    @Transactional
    public TemperatureLogDto logTemperature(Long batchId, LogTemperatureRequest req) {
        FreshProcessingBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        AppUser user = null;
        if (req.getLoggedByUserId() != null) {
            user = userRepository.findById(req.getLoggedByUserId()).orElse(null);
        }

        FreshBatchTemperatureLog log = FreshBatchTemperatureLog.builder()
                .batch(batch)
                .loggedTempCelsius(req.getLoggedTempCelsius())
                .locationTag(req.getLocationTag())
                .loggedByUser(user)
                .build();

        temperatureLogRepository.save(log);

        // Update current storage temp on batch
        batch.setStorageTempCelsius(req.getLoggedTempCelsius());
        batchRepository.save(batch);

        return toTemperatureLogDto(log);
    }

    public BatchDto getBatch(Long batchId) {
        FreshProcessingBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));
        return toBatchDto(batch);
    }

    public List<BatchDto> getBatchesByVendor(Long vendorId) {
        return batchRepository.findByVendorIdOrderByProductionDateDesc(vendorId).stream()
                .map(this::toBatchDto)
                .collect(Collectors.toList());
    }

    public List<BatchDto> getBatchesByVendorAndStatus(Long vendorId, BatchStatus status) {
        return batchRepository.findByVendorIdAndStatus(vendorId, status).stream()
                .map(this::toBatchDto)
                .collect(Collectors.toList());
    }

    // ----- DTO Mappers -----

    private RecipeDto toRecipeDto(FreshProcessingRecipe r) {
        return RecipeDto.builder()
                .id(r.getId())
                .vendorId(r.getVendor().getId())
                .recipeCode(r.getRecipeCode())
                .recipeName(r.getRecipeName())
                .outputProductId(r.getOutputProduct().getId())
                .outputProductName(r.getOutputProduct().getName())
                .targetOutputUnit(r.getTargetOutputUnit())
                .expectedYieldPercent(r.getExpectedYieldPercent())
                .storageTempMinCelsius(r.getStorageTempMinCelsius())
                .storageTempMaxCelsius(r.getStorageTempMaxCelsius())
                .shelfLifeHours(r.getShelfLifeHours())
                .instructions(r.getInstructions())
                .build();
    }

    private BatchDto toBatchDto(FreshProcessingBatch b) {
        BatchReleaseDto releaseDto = batchReleaseRepository.findByBatchId(b.getId())
                .map(this::toBatchReleaseDto)
                .orElse(null);

        List<QualityCheckDto> qcs = qualityCheckRepository.findByBatchIdOrderByCheckedAtAsc(b.getId()).stream()
                .map(this::toQualityCheckDto)
                .collect(Collectors.toList());

        return BatchDto.builder()
                .id(b.getId())
                .batchNumber(b.getBatchNumber())
                .vendorId(b.getVendor().getId())
                .recipeId(b.getRecipe() != null ? b.getRecipe().getId() : null)
                .recipeName(b.getRecipe() != null ? b.getRecipe().getRecipeName() : null)
                .vendorProductId(b.getVendorProduct().getId())
                .productName(b.getVendorProduct().getName())
                .productionDate(b.getProductionDate())
                .rawMaterialInputQty(b.getRawMaterialInputQty())
                .actualOutputQty(b.getActualOutputQty())
                .actualYieldPercent(b.getActualYieldPercent())
                .wastageQty(b.getWastageQty())
                .wastageReason(b.getWastageReason())
                .status(b.getStatus())
                .storageTempCelsius(b.getStorageTempCelsius())
                .expiryTimestamp(b.getExpiryTimestamp())
                .notes(b.getNotes())
                .createdAt(b.getCreatedAt())
                .release(releaseDto)
                .qualityChecks(qcs)
                .build();
    }

    private QualityCheckDto toQualityCheckDto(FreshBatchQualityCheck q) {
        return QualityCheckDto.builder()
                .id(q.getId())
                .batchId(q.getBatch().getId())
                .checkName(q.getCheckName())
                .measuredValue(q.getMeasuredValue())
                .isPassed(q.getIsPassed())
                .checkedByUserId(q.getCheckedByUser().getId())
                .checkedByUserName(q.getCheckedByUser().getFullName() != null ? q.getCheckedByUser().getFullName() : q.getCheckedByUser().getEmail())
                .checkedAt(q.getCheckedAt())
                .notes(q.getNotes())
                .build();
    }

    private BatchReleaseDto toBatchReleaseDto(FreshBatchRelease r) {
        return BatchReleaseDto.builder()
                .id(r.getId())
                .batchId(r.getBatch().getId())
                .releasedByUserId(r.getReleasedByUser().getId())
                .releasedByUserName(r.getReleasedByUser().getFullName() != null ? r.getReleasedByUser().getFullName() : r.getReleasedByUser().getEmail())
                .releaseDecision(r.getReleaseDecision())
                .temperatureVerifiedCelsius(r.getTemperatureVerifiedCelsius())
                .releasedAt(r.getReleasedAt())
                .remarks(r.getRemarks())
                .build();
    }

    private TemperatureLogDto toTemperatureLogDto(FreshBatchTemperatureLog t) {
        return TemperatureLogDto.builder()
                .id(t.getId())
                .batchId(t.getBatch().getId())
                .loggedTempCelsius(t.getLoggedTempCelsius())
                .locationTag(t.getLocationTag())
                .loggedByUserId(t.getLoggedByUser() != null ? t.getLoggedByUser().getId() : null)
                .loggedAt(t.getLoggedAt())
                .build();
    }
}