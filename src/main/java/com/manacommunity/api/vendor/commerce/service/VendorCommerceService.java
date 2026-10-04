package com.manacommunity.api.vendor.commerce.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.commerce.dto.*;
import com.manacommunity.api.vendor.commerce.model.*;
import com.manacommunity.api.vendor.commerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorCommerceService {

    private final VendorProductRepository productRepository;
    private final VendorProductVariantRepository variantRepository;
    private final VendorProductInventoryRepository inventoryRepository;
    private final VendorInventoryBatchRepository batchRepository;

    public List<VendorProductDto> getVendorProducts(Long vendorUserId) {
        List<VendorProduct> products = productRepository.findByVendorUserIdOrderByCreatedAtDesc(vendorUserId);
        return products.stream().map(this::mapToProductDto).collect(Collectors.toList());
    }

    public VendorProductDto getProductById(Long productId) {
        VendorProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        return mapToProductDto(product);
    }

    @Transactional
    public VendorProductDto createProduct(AppUser vendorUser, CreateProductRequest request) {
        Community community = vendorUser.getCommunity();

        VendorProduct product = VendorProduct.builder()
                .community(community)
                .vendorUser(vendorUser)
                .name(request.getName())
                .category(request.getCategory())
                .subCategory(request.getSubCategory())
                .brand(request.getBrand())
                .description(request.getDescription())
                .hsnCode(request.getHsnCode())
                .gstRate(request.getGstRate() != null ? request.getGstRate() : BigDecimal.valueOf(5.0))
                .imageUrl(request.getImageUrl())
                .status("ACTIVE")
                .build();

        productRepository.save(product);

        for (CreateProductRequest.CreateVariantRequest vr : request.getVariants()) {
            VendorProductVariant variant = VendorProductVariant.builder()
                    .product(product)
                    .variantName(vr.getVariantName())
                    .sku(vr.getSku())
                    .barcode(vr.getBarcode())
                    .packSize(vr.getPackSize())
                    .mrp(vr.getMrp())
                    .vendorCost(vr.getVendorCost())
                    .defaultCommunityPrice(vr.getDefaultCommunityPrice())
                    .isActive(true)
                    .build();

            variantRepository.save(variant);

            int initialStock = vr.getInitialStock() != null ? vr.getInitialStock() : 0;
            VendorProductInventory inventory = VendorProductInventory.builder()
                    .variant(variant)
                    .availableQty(initialStock)
                    .reservedQty(0)
                    .committedQty(0)
                    .allocatedQty(0)
                    .pickedQty(0)
                    .dispatchedQty(0)
                    .deliveredQty(0)
                    .damagedQty(0)
                    .build();

            inventoryRepository.save(inventory);

            if (initialStock > 0) {
                VendorInventoryBatch batch = VendorInventoryBatch.builder()
                        .variant(variant)
                        .batchNumber("BATCH-" + System.currentTimeMillis())
                        .receivedQty(initialStock)
                        .remainingQty(initialStock)
                        .status("AVAILABLE")
                        .build();
                batchRepository.save(batch);
            }

            product.getVariants().add(variant);
        }

        return mapToProductDto(product);
    }

    @Transactional
    public InventoryReservationResponse reserveInventory(InventoryReservationRequest request) {
        Long variantId = request.getVariantId();
        int requestedQty = request.getQuantity();

        // Pessimistic Lock prevents concurrency race condition
        VendorProductInventory inventory = inventoryRepository.findByVariantIdForUpdate(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for variant: " + variantId));

        if (inventory.getAvailableQty() < requestedQty) {
            return InventoryReservationResponse.builder()
                    .success(false)
                    .message("Insufficient stock. Only " + inventory.getAvailableQty() + " units available.")
                    .variantId(variantId)
                    .reservedQty(0)
                    .remainingAvailableQty(inventory.getAvailableQty())
                    .build();
        }

        // Atomic transition: AVAILABLE -> RESERVED
        inventory.setAvailableQty(inventory.getAvailableQty() - requestedQty);
        inventory.setReservedQty(inventory.getReservedQty() + requestedQty);
        inventoryRepository.save(inventory);

        return InventoryReservationResponse.builder()
                .success(true)
                .message("Successfully reserved " + requestedQty + " units.")
                .variantId(variantId)
                .reservedQty(requestedQty)
                .remainingAvailableQty(inventory.getAvailableQty())
                .build();
    }

    @Transactional
    public void commitInventory(Long variantId, int qty) {
        VendorProductInventory inventory = inventoryRepository.findByVariantIdForUpdate(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for variant: " + variantId));

        int toCommit = Math.min(inventory.getReservedQty(), qty);
        inventory.setReservedQty(inventory.getReservedQty() - toCommit);
        inventory.setCommittedQty(inventory.getCommittedQty() + toCommit);
        inventoryRepository.save(inventory);
    }

    @Transactional
    public void releaseReservation(Long variantId, int qty) {
        VendorProductInventory inventory = inventoryRepository.findByVariantIdForUpdate(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for variant: " + variantId));

        int toRelease = Math.min(inventory.getReservedQty(), qty);
        inventory.setReservedQty(inventory.getReservedQty() - toRelease);
        inventory.setAvailableQty(inventory.getAvailableQty() + toRelease);
        inventoryRepository.save(inventory);
    }

    public VendorCommerceStatsDto getStats(Long vendorUserId) {
        List<VendorProduct> products = productRepository.findByVendorUserIdOrderByCreatedAtDesc(vendorUserId);
        int active = (int) products.stream().filter(p -> "ACTIVE".equals(p.getStatus())).count();
        int draft = (int) products.stream().filter(p -> "DRAFT".equals(p.getStatus())).count();

        int totalUnits = 0;
        int reserved = 0;
        int committed = 0;
        int outOfStock = 0;

        for (VendorProduct p : products) {
            boolean hasStock = false;
            for (VendorProductVariant v : p.getVariants()) {
                if (v.getInventory() != null) {
                    totalUnits += v.getInventory().getAvailableQty();
                    reserved += v.getInventory().getReservedQty();
                    committed += v.getInventory().getCommittedQty();
                    if (v.getInventory().getAvailableQty() > 0) hasStock = true;
                }
            }
            if (!hasStock) outOfStock++;
        }

        return VendorCommerceStatsDto.builder()
                .totalProducts(products.size())
                .activeProducts(active)
                .outOfStockProducts(outOfStock)
                .draftProducts(draft)
                .totalInventoryUnits(totalUnits)
                .reservedUnits(reserved)
                .committedUnits(committed)
                .build();
    }

    private VendorProductDto mapToProductDto(VendorProduct product) {
        int totalStock = 0;
        List<ProductVariantDto> variantDtos = new ArrayList<>();

        if (product.getVariants() != null) {
            for (VendorProductVariant v : product.getVariants()) {
                int avail = v.getInventory() != null ? v.getInventory().getAvailableQty() : 0;
                int res = v.getInventory() != null ? v.getInventory().getReservedQty() : 0;
                int comm = v.getInventory() != null ? v.getInventory().getCommittedQty() : 0;
                totalStock += avail;

                variantDtos.add(ProductVariantDto.builder()
                        .id(String.valueOf(v.getId()))
                        .variantName(v.getVariantName())
                        .sku(v.getSku())
                        .barcode(v.getBarcode())
                        .packSize(v.getPackSize())
                        .mrp(v.getMrp())
                        .vendorCost(v.getVendorCost())
                        .defaultCommunityPrice(v.getDefaultCommunityPrice())
                        .isActive(v.getIsActive())
                        .availableQty(avail)
                        .reservedQty(res)
                        .committedQty(comm)
                        .build());
            }
        }

        return VendorProductDto.builder()
                .id(String.valueOf(product.getId()))
                .name(product.getName())
                .category(product.getCategory())
                .subCategory(product.getSubCategory())
                .brand(product.getBrand())
                .description(product.getDescription())
                .hsnCode(product.getHsnCode())
                .gstRate(product.getGstRate())
                .status(product.getStatus())
                .imageUrl(product.getImageUrl())
                .variants(variantDtos)
                .totalStock(totalStock)
                .build();
    }
}
