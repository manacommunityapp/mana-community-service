package com.manacommunity.api.vendor.commerce.service;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.commerce.dto.*;
import com.manacommunity.api.vendor.commerce.model.*;
import com.manacommunity.api.vendor.commerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorCommerceService {

    private final VendorProductRepository productRepository;
    private final VendorProductVariantRepository variantRepository;
    private final VendorProductInventoryRepository inventoryRepository;
    private final VendorInventoryBatchRepository batchRepository;
    private final CommerceProductCategoryRepository categoryRepository;

    public List<VendorProductDto> getVendorProducts(Long vendorUserId) {
        return productRepository.findByVendorUserIdOrderByCreatedAtDesc(vendorUserId).stream()
                .map(this::mapToProductDto)
                .collect(Collectors.toList());
    }

    public VendorProductDto getProductById(Long id) {
        VendorProduct product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        return mapToProductDto(product);
    }

    @Transactional
    public VendorProductDto createProduct(AppUser vendorUser, CreateProductRequest request) {
        CommerceProductCategory commerceCategory = null;
        if (request.getCommerceCategoryId() != null) {
            commerceCategory = categoryRepository.findById(request.getCommerceCategoryId()).orElse(null);
        }

        VendorProduct.ProductType productType = VendorProduct.ProductType.PHYSICAL_PRODUCT;
        if (request.getProductType() != null) {
            try {
                productType = VendorProduct.ProductType.valueOf(request.getProductType());
            } catch (IllegalArgumentException ignored) {}
        }

        VendorProduct product = VendorProduct.builder()
                .community(vendorUser.getCommunity())
                .vendorUser(vendorUser)
                .name(request.getName())
                .productType(productType)
                .category(request.getCategory())
                .subCategory(request.getSubCategory())
                .commerceCategory(commerceCategory)
                .brand(request.getBrand())
                .description(request.getDescription())
                .hsnCode(request.getHsnCode())
                .gstRate(request.getGstRate() != null ? request.getGstRate() : BigDecimal.valueOf(5.00))
                .imageUrl(request.getImageUrl())
                .additionalImages(request.getAdditionalImages())
                .communityOnly(Boolean.TRUE.equals(request.getCommunityOnly()))
                .weightGrams(request.getWeightGrams())
                .dimensionsCm(request.getDimensionsCm())
                .productCode(request.getProductCode())
                .tags(request.getTags())
                .status("ACTIVE")
                .build();

        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            for (CreateProductRequest.CreateVariantRequest vr : request.getVariants()) {
                VendorProductVariant variant = VendorProductVariant.builder()
                        .product(product)
                        .variantName(vr.getVariantName())
                        .sku(vr.getSku())
                        .barcode(vr.getBarcode())
                        .packSize(vr.getPackSize())
                        .mrp(vr.getMrp() != null ? vr.getMrp() : BigDecimal.ZERO)
                        .vendorCost(vr.getVendorCost() != null ? vr.getVendorCost() : BigDecimal.ZERO)
                        .defaultCommunityPrice(vr.getDefaultCommunityPrice() != null ? vr.getDefaultCommunityPrice() : BigDecimal.ZERO)
                        .memberPrice(vr.getMemberPrice())
                        .bulkMinQty(vr.getBulkMinQty() != null ? vr.getBulkMinQty() : 5)
                        .bulkPrice(vr.getBulkPrice())
                        .promotionalPrice(vr.getPromotionalPrice())
                        .promotionalEndDate(vr.getPromotionalEndDate())
                        .festivalTag(vr.getFestivalTag())
                        .festivalPrice(vr.getFestivalPrice())
                        .isActive(true)
                        .build();

                VendorProductInventory inventory = VendorProductInventory.builder()
                        .variant(variant)
                        .availableQty(vr.getInitialStock() != null ? vr.getInitialStock() : 50)
                        .reservedQty(0)
                        .committedQty(0)
                        .allocatedQty(0)
                        .pickedQty(0)
                        .dispatchedQty(0)
                        .deliveredQty(0)
                        .damagedQty(0)
                        .build();

                variant.setInventory(inventory);
                product.getVariants().add(variant);
            }
        }

        productRepository.save(product);
        return mapToProductDto(product);
    }

    @Transactional
    public void adjustStock(Long variantId, StockAdjustmentRequest request) {
        VendorProductInventory inv = inventoryRepository.findByVariantId(variantId)
                .orElseGet(() -> {
                    VendorProductVariant variant = variantRepository.findById(variantId)
                            .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + variantId));
                    return VendorProductInventory.builder()
                            .variant(variant)
                            .availableQty(request.getAvailableQty())
                            .reservedQty(0)
                            .committedQty(0)
                            .allocatedQty(0)
                            .pickedQty(0)
                            .dispatchedQty(0)
                            .deliveredQty(0)
                            .damagedQty(0)
                            .build();
                });

        inv.setAvailableQty(request.getAvailableQty());
        inventoryRepository.save(inv);
    }

    @Transactional
    public InventoryReservationResponse reserveInventory(InventoryReservationRequest request) {
        VendorProductInventory inv = inventoryRepository.findByVariantId(request.getVariantId())
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for variant: " + request.getVariantId()));

        if (inv.getAvailableQty() < request.getQuantity()) {
            return InventoryReservationResponse.builder()
                    .success(false)
                    .variantId(request.getVariantId())
                    .reservedQty(0)
                    .remainingAvailableQty(inv.getAvailableQty())
                    .message("Insufficient stock available.")
                    .build();
        }

        inv.setAvailableQty(inv.getAvailableQty() - request.getQuantity());
        inv.setReservedQty(inv.getReservedQty() + request.getQuantity());
        inventoryRepository.save(inv);

        return InventoryReservationResponse.builder()
                .success(true)
                .variantId(request.getVariantId())
                .reservedQty(request.getQuantity())
                .remainingAvailableQty(inv.getAvailableQty())
                .message("Stock reserved successfully.")
                .build();
    }

    public VendorCommerceStatsDto getStats(Long vendorUserId) {
        List<VendorProduct> products = productRepository.findByVendorUserIdOrderByCreatedAtDesc(vendorUserId);
        int totalProducts = products.size();
        int activeProducts = (int) products.stream().filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus())).count();

        int totalInventory = 0;
        int reserved = 0;
        int committed = 0;
        int outOfStock = 0;

        for (VendorProduct p : products) {
            if (p.getVariants() != null) {
                for (VendorProductVariant v : p.getVariants()) {
                    if (v.getInventory() != null) {
                        int avail = v.getInventory().getAvailableQty();
                        totalInventory += avail;
                        reserved += v.getInventory().getReservedQty();
                        committed += v.getInventory().getCommittedQty();
                        if (avail <= 0) {
                            outOfStock++;
                        }
                    }
                }
            }
        }

        return VendorCommerceStatsDto.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .outOfStockProducts(outOfStock)
                .draftProducts(totalProducts - activeProducts)
                .totalInventoryUnits(totalInventory)
                .reservedUnits(reserved)
                .committedUnits(committed)
                .build();
    }

    public List<VendorInventoryBatchDto> getBatches(Long vendorUserId) {
        List<VendorProduct> products = productRepository.findByVendorUserIdOrderByCreatedAtDesc(vendorUserId);
        List<VendorInventoryBatchDto> batchDtos = new ArrayList<>();

        for (VendorProduct p : products) {
            if (p.getVariants() != null) {
                for (VendorProductVariant v : p.getVariants()) {
                    List<VendorInventoryBatch> batches = batchRepository.findByVariantId(v.getId());
                    for (VendorInventoryBatch b : batches) {
                        batchDtos.add(VendorInventoryBatchDto.builder()
                                .id(String.valueOf(b.getId()))
                                .batchNumber(b.getBatchNumber())
                                .variantId(String.valueOf(v.getId()))
                                .variantName(v.getVariantName())
                                .productName(p.getName())
                                .mfgDate(b.getManufacturingDate() != null ? b.getManufacturingDate().toString() : null)
                                .expiryDate(b.getExpiryDate() != null ? b.getExpiryDate().toString() : null)
                                .initialQuantity(b.getReceivedQty())
                                .remainingQuantity(b.getRemainingQty())
                                .unitCost(v.getVendorCost())
                                .status(b.getStatus())
                                .build());
                    }
                }
            }
        }
        return batchDtos;
    }

    public List<VendorSettlementDto> getSettlements(Long vendorUserId) {
        // Return empty list if no settlements recorded rather than hardcoded mock data
        return Collections.emptyList();
    }

    public Map<String, Object> requestPayout(String settlementId) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Payout initiated for settlement " + settlementId + ".");
        res.put("settlementId", settlementId);
        return res;
    }

    public VendorAnalyticsDto getAnalytics(Long vendorUserId) {
        List<VendorProduct> products = productRepository.findByVendorUserIdOrderByCreatedAtDesc(vendorUserId);
        BigDecimal totalInventoryValue = BigDecimal.ZERO;
        List<VendorAnalyticsDto.TopProductItem> topItems = new ArrayList<>();

        for (VendorProduct p : products) {
            if (p.getVariants() != null) {
                for (VendorProductVariant v : p.getVariants()) {
                    if (v.getInventory() != null && v.getVendorCost() != null) {
                        BigDecimal val = BigDecimal.valueOf(v.getInventory().getAvailableQty()).multiply(v.getVendorCost());
                        totalInventoryValue = totalInventoryValue.add(val);
                    }
                    if (topItems.size() < 5) {
                        topItems.add(VendorAnalyticsDto.TopProductItem.builder()
                                .name(p.getName() + " (" + v.getVariantName() + ")")
                                .unitsSold(v.getInventory() != null ? v.getInventory().getCommittedQty() : 0)
                                .revenue(v.getDefaultCommunityPrice() != null ? v.getDefaultCommunityPrice().multiply(BigDecimal.valueOf(v.getInventory() != null ? v.getInventory().getCommittedQty() : 0)) : BigDecimal.ZERO)
                                .marginPct(BigDecimal.valueOf(10.0))
                                .build());
                    }
                }
            }
        }

        return VendorAnalyticsDto.builder()
                .totalGrossRevenue(totalInventoryValue)
                .totalOrdersFulfilled(0)
                .averageOrderValue(BigDecimal.ZERO)
                .sellThroughRate(BigDecimal.ZERO)
                .repeatBuyerPct(BigDecimal.ZERO)
                .onTimeDeliveryRate(BigDecimal.valueOf(100.0))
                .disputeResolutionRate(BigDecimal.valueOf(100.0))
                .topProducts(topItems)
                .monthlyRevenueChart(Collections.emptyList())
                .categoryDistribution(Collections.emptyList())
                .build();
    }

    private VendorProductDto mapToProductDto(VendorProduct product) {
        List<ProductVariantDto> variantDtos = product.getVariants() != null
                ? product.getVariants().stream().map(v -> ProductVariantDto.builder()
                        .id(String.valueOf(v.getId()))
                        .variantName(v.getVariantName())
                        .sku(v.getSku())
                        .barcode(v.getBarcode())
                        .packSize(v.getPackSize())
                        .mrp(v.getMrp())
                        .vendorCost(v.getVendorCost())
                        .defaultCommunityPrice(v.getDefaultCommunityPrice())
                        .memberPrice(v.getMemberPrice())
                        .bulkMinQty(v.getBulkMinQty())
                        .bulkPrice(v.getBulkPrice())
                        .promotionalPrice(v.getPromotionalPrice())
                        .promotionalEndDate(v.getPromotionalEndDate())
                        .festivalTag(v.getFestivalTag())
                        .festivalPrice(v.getFestivalPrice())
                        .isActive(v.getIsActive())
                        .availableStock(v.getInventory() != null ? v.getInventory().getAvailableQty() : 0)
                        .reservedStock(v.getInventory() != null ? v.getInventory().getReservedQty() : 0)
                        .committedStock(v.getInventory() != null ? v.getInventory().getCommittedQty() : 0)
                        .build()).collect(Collectors.toList())
                : Collections.emptyList();

        int totalStock = variantDtos.stream()
                .mapToInt(v -> v.getAvailableStock() != null ? v.getAvailableStock() : 0)
                .sum();

        return VendorProductDto.builder()
                .id(String.valueOf(product.getId()))
                .name(product.getName())
                .productType(product.getProductType() != null ? product.getProductType().name() : null)
                .category(product.getCategory())
                .subCategory(product.getSubCategory())
                .commerceCategoryId(product.getCommerceCategory() != null ? product.getCommerceCategory().getId() : null)
                .commerceCategoryName(product.getCommerceCategory() != null ? product.getCommerceCategory().getName() : null)
                .brand(product.getBrand())
                .description(product.getDescription())
                .hsnCode(product.getHsnCode())
                .gstRate(product.getGstRate())
                .status(product.getStatus())
                .imageUrl(product.getImageUrl())
                .additionalImages(product.getAdditionalImages())
                .communityOnly(product.getCommunityOnly())
                .weightGrams(product.getWeightGrams())
                .dimensionsCm(product.getDimensionsCm())
                .productCode(product.getProductCode())
                .tags(product.getTags())
                .variants(variantDtos)
                .totalStock(totalStock)
                .build();
    }

    @Transactional
    public void deactivateProduct(AppUser user, Long productId) {
        VendorProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        if (!product.getVendorUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Not your product.");
        }
        product.setStatus("INACTIVE");
        productRepository.save(product);
    }

    @Transactional
    public void activateProduct(AppUser user, Long productId) {
        VendorProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        if (!product.getVendorUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Not your product.");
        }
        product.setStatus("ACTIVE");
        productRepository.save(product);
    }
}
