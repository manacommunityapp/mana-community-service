package com.manacommunity.api.freshkitchen.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.freshkitchen.dto.VendorCapacityDto;
import com.manacommunity.api.freshkitchen.dto.VendorDailyManifestDto;
import com.manacommunity.api.freshkitchen.model.FoodProductionBatch;
import com.manacommunity.api.freshkitchen.model.VendorProductionCapacity;
import com.manacommunity.api.freshkitchen.repository.FoodProductionBatchRepository;
import com.manacommunity.api.freshkitchen.repository.VendorProductionCapacityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorProductionCapacityService {

    private final VendorProductionCapacityRepository capacityRepository;
    private final FoodProductionBatchRepository batchRepository;

    @Transactional(readOnly = true)
    public List<VendorCapacityDto> getCapacityForVendor(String vendorId, LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return capacityRepository.findByVendorIdAndProductionDate(vendorId, queryDate)
                .stream()
                .map(VendorCapacityDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VendorCapacityDto> getCapacityForDate(LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return capacityRepository.findByProductionDate(queryDate)
                .stream()
                .map(VendorCapacityDto::fromEntity)
                .toList();
    }

    @Transactional
    public VendorCapacityDto setOrUpdateCapacity(VendorCapacityDto request) {
        if (request.getVendorId() == null || request.getProductId() == null || request.getProductionDate() == null) {
            throw new InvalidInputException("vendorId, productId, and productionDate are required");
        }

        VendorProductionCapacity capacity = capacityRepository
                .findByVendorIdAndProductIdAndProductionDate(
                        request.getVendorId(),
                        request.getProductId(),
                        request.getProductionDate()
                )
                .orElse(null);

        if (capacity == null) {
            int maxCap = request.getMaxCapacity() != null ? request.getMaxCapacity() : 0;
            int reserved = request.getReservedQuantity() != null ? request.getReservedQuantity() : 0;
            int available = Math.max(0, maxCap - reserved);
            boolean soldOut = Boolean.TRUE.equals(request.getIsSoldOut()) || available <= 0;

            capacity = VendorProductionCapacity.builder()
                    .vendorId(request.getVendorId())
                    .vendorName(request.getVendorName())
                    .productId(request.getProductId())
                    .productName(request.getProductName())
                    .productionDate(request.getProductionDate())
                    .maxCapacity(maxCap)
                    .reservedQuantity(reserved)
                    .availableQuantity(available)
                    .isSoldOut(soldOut)
                    .build();
        } else {
            // Optimistic lock check if client supplied a version
            if (request.getVersion() != null && !request.getVersion().equals(capacity.getVersion())) {
                throw new OptimisticLockingFailureException(
                        "Vendor capacity has been modified by another transaction. Version mismatch: "
                                + "expected " + request.getVersion() + ", actual " + capacity.getVersion());
            }

            if (request.getVendorName() != null) {
                capacity.setVendorName(request.getVendorName());
            }
            if (request.getProductName() != null) {
                capacity.setProductName(request.getProductName());
            }
            if (request.getMaxCapacity() != null) {
                capacity.setMaxCapacity(request.getMaxCapacity());
            }
            if (request.getReservedQuantity() != null) {
                capacity.setReservedQuantity(request.getReservedQuantity());
            }

            int available = Math.max(0, capacity.getMaxCapacity() - capacity.getReservedQuantity());
            capacity.setAvailableQuantity(available);

            if (request.getIsSoldOut() != null) {
                capacity.setIsSoldOut(request.getIsSoldOut());
            } else {
                capacity.setIsSoldOut(available <= 0);
            }
        }

        capacity = capacityRepository.save(capacity);
        return VendorCapacityDto.fromEntity(capacity);
    }

    /**
     * Check and reserve capacity for a specific vendor, product, and production date with optimistic lock enforcement.
     */
    @Transactional
    public boolean checkAndReserveCapacity(String vendorId, String productId, LocalDate productionDate, int quantity) {
        Optional<VendorProductionCapacity> opt = capacityRepository
                .findByVendorIdAndProductIdAndProductionDate(vendorId, productId, productionDate);

        if (opt.isEmpty()) {
            log.info("No explicit capacity set for vendor {} product {} on {}. Treating as unconstrained.",
                    vendorId, productId, productionDate);
            return true;
        }

        VendorProductionCapacity capacity = opt.get();
        if (Boolean.TRUE.equals(capacity.getIsSoldOut()) || capacity.getAvailableQuantity() < quantity) {
            log.warn("Capacity exceeded for vendor {} product {} on {}. Available: {}, Requested: {}",
                    vendorId, productId, productionDate, capacity.getAvailableQuantity(), quantity);
            return false;
        }

        capacity.setReservedQuantity(capacity.getReservedQuantity() + quantity);
        int available = Math.max(0, capacity.getMaxCapacity() - capacity.getReservedQuantity());
        capacity.setAvailableQuantity(available);
        if (available <= 0) {
            capacity.setIsSoldOut(true);
        }

        capacityRepository.save(capacity);
        return true;
    }

    /**
     * Finds any available vendor capacity for the given product on the production date and reserves it.
     */
    @Transactional
    public boolean checkAndReserveAnyCapacity(String productId, LocalDate productionDate, int quantity) {
        List<VendorProductionCapacity> list = capacityRepository.findByProductIdAndProductionDate(productId, productionDate);
        if (list.isEmpty()) {
            log.debug("No capacity records configured for product {} on {}. Defaulting to accepted.", productId, productionDate);
            return true;
        }

        for (VendorProductionCapacity cap : list) {
            if (!Boolean.TRUE.equals(cap.getIsSoldOut()) && cap.getAvailableQuantity() >= quantity) {
                cap.setReservedQuantity(cap.getReservedQuantity() + quantity);
                int available = Math.max(0, cap.getMaxCapacity() - cap.getReservedQuantity());
                cap.setAvailableQuantity(available);
                if (available <= 0) {
                    cap.setIsSoldOut(true);
                }
                capacityRepository.save(cap);
                return true;
            }
        }

        log.warn("All vendor capacities exhausted for product {} on {}. Quantity requested: {}",
                productId, productionDate, quantity);
        return false;
    }

    @Transactional
    public VendorCapacityDto autoMarkSoldOut(Long capacityId) {
        VendorProductionCapacity capacity = capacityRepository.findById(capacityId)
                .orElseThrow(() -> new ResourceNotFoundException("VendorProductionCapacity", capacityId));

        capacity.setIsSoldOut(true);
        capacity.setAvailableQuantity(0);
        capacity = capacityRepository.save(capacity);
        return VendorCapacityDto.fromEntity(capacity);
    }

    @Transactional(readOnly = true)
    public VendorDailyManifestDto getVendorDailyManifest(String vendorId, LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        List<VendorProductionCapacity> capacities = capacityRepository.findByVendorIdAndProductionDate(vendorId, queryDate);
        List<FoodProductionBatch> batches = batchRepository.findByVendorIdAndProductionDate(vendorId, queryDate);

        String vendorName = capacities.isEmpty() ? vendorId : capacities.get(0).getVendorName();
        int totalMax = 0;
        int totalReserved = 0;
        int totalAvailable = 0;

        List<VendorDailyManifestDto.ManifestItemDto> items = new ArrayList<>();
        for (VendorProductionCapacity cap : capacities) {
            totalMax += cap.getMaxCapacity();
            totalReserved += cap.getReservedQuantity();
            totalAvailable += cap.getAvailableQuantity();

            List<String> matchingBatches = batches.stream()
                    .filter(b -> b.getProductId().equals(cap.getProductId()))
                    .map(FoodProductionBatch::getBatchNumber)
                    .toList();

            items.add(VendorDailyManifestDto.ManifestItemDto.builder()
                    .productId(cap.getProductId())
                    .productName(cap.getProductName())
                    .maxCapacity(cap.getMaxCapacity())
                    .reservedQuantity(cap.getReservedQuantity())
                    .availableQuantity(cap.getAvailableQuantity())
                    .isSoldOut(cap.getIsSoldOut())
                    .batchNumbers(matchingBatches)
                    .build());
        }

        return VendorDailyManifestDto.builder()
                .vendorId(vendorId)
                .vendorName(vendorName)
                .productionDate(queryDate)
                .totalMaxCapacity(totalMax)
                .totalReservedQuantity(totalReserved)
                .totalAvailableQuantity(totalAvailable)
                .items(items)
                .build();
    }

    @Transactional(readOnly = true)
    public String exportVendorDailyManifestCsv(String vendorId, LocalDate date) {
        VendorDailyManifestDto manifest = getVendorDailyManifest(vendorId, date);
        StringBuilder csv = new StringBuilder();
        csv.append("Vendor ID,Vendor Name,Production Date,Product ID,Product Name,Max Capacity,Reserved Qty,Available Qty,Is Sold Out,Batches\n");

        for (VendorDailyManifestDto.ManifestItemDto item : manifest.getItems()) {
            csv.append('"').append(manifest.getVendorId()).append("\",");
            csv.append('"').append(manifest.getVendorName() != null ? manifest.getVendorName() : "").append("\",");
            csv.append('"').append(manifest.getProductionDate()).append("\",");
            csv.append('"').append(item.getProductId()).append("\",");
            csv.append('"').append(item.getProductName() != null ? item.getProductName().replace("\"", "\"\"") : "").append("\",");
            csv.append(item.getMaxCapacity()).append(',');
            csv.append(item.getReservedQuantity()).append(',');
            csv.append(item.getAvailableQuantity()).append(',');
            csv.append(Boolean.TRUE.equals(item.getIsSoldOut()) ? "YES" : "NO").append(',');
            csv.append('"').append(item.getBatchNumbers() != null ? String.join("; ", item.getBatchNumbers()) : "").append("\"\n");
        }

        return csv.toString();
    }
}
