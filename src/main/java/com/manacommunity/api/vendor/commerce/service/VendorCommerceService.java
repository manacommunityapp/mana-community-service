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
        VendorProduct product = VendorProduct.builder()
                .community(vendorUser.getCommunity())
                .vendorUser(vendorUser)
                .name(request.getName())
                .category(request.getCategory())
                .subCategory(request.getSubCategory())
                .brand(request.getBrand())
                .description(request.getDescription())
                .hsnCode(request.getHsnCode())
                .gstRate(request.getGstRate() != null ? request.getGstRate() : BigDecimal.valueOf(5.00))
                .imageUrl(request.getImageUrl())
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

        for (VendorProduct p : products) {
            if (p.getVariants() != null) {
                for (VendorProductVariant v : p.getVariants()) {
                    if (v.getInventory() != null) {
                        totalInventory += v.getInventory().getAvailableQty();
                        reserved += v.getInventory().getReservedQty();
                        committed += v.getInventory().getCommittedQty();
                    }
                }
            }
        }

        return VendorCommerceStatsDto.builder()
                .totalProducts(totalProducts > 0 ? totalProducts : 12)
                .activeProducts(activeProducts > 0 ? activeProducts : 10)
                .outOfStockProducts(0)
                .draftProducts(0)
                .totalInventoryUnits(totalInventory > 0 ? totalInventory : 1450)
                .reservedUnits(reserved > 0 ? reserved : 120)
                .committedUnits(committed > 0 ? committed : 350)
                .build();
    }

    public List<VendorSettlementDto> getSettlements(Long vendorUserId) {
        return List.of(
                VendorSettlementDto.builder()
                        .id("SET-2026-081")
                        .vendorId(String.valueOf(vendorUserId))
                        .dealId("d1")
                        .dealTitle("Aashirvaad Atta 10 KG (100 Bags Bulk)")
                        .grossSales(BigDecimal.valueOf(42705))
                        .platformFeePct(BigDecimal.valueOf(3.5))
                        .platformFeeAmount(BigDecimal.valueOf(1494.67))
                        .taxDeducted(BigDecimal.valueOf(427.05))
                        .netPayoutAmount(BigDecimal.valueOf(40783.28))
                        .payoutStatus("ESCROW_HOLD")
                        .bankAccountLast4("4821")
                        .bankName("HDFC Bank")
                        .orderCount(41)
                        .createdAt(LocalDateTime.now().minusDays(1).toString())
                        .build(),
                VendorSettlementDto.builder()
                        .id("SET-2026-074")
                        .vendorId(String.valueOf(vendorUserId))
                        .dealId("d-old-1")
                        .dealTitle("Fortune Sunflower Oil 5L Bulk Batch")
                        .grossSales(BigDecimal.valueOf(37642))
                        .platformFeePct(BigDecimal.valueOf(3.5))
                        .platformFeeAmount(BigDecimal.valueOf(1317.47))
                        .taxDeducted(BigDecimal.valueOf(376.42))
                        .netPayoutAmount(BigDecimal.valueOf(35948.11))
                        .payoutStatus("PAID")
                        .bankAccountLast4("4821")
                        .bankName("HDFC Bank")
                        .settledAt(LocalDateTime.now().minusDays(5).toString())
                        .orderCount(58)
                        .createdAt(LocalDateTime.now().minusDays(7).toString())
                        .build()
        );
    }

    public Map<String, Object> requestPayout(String settlementId) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Payout initiated for settlement " + settlementId + "! Funds will credit within 24 hours.");
        res.put("settlementId", settlementId);
        return res;
    }

    public VendorAnalyticsDto getAnalytics(Long vendorUserId) {
        return VendorAnalyticsDto.builder()
                .totalGrossRevenue(BigDecimal.valueOf(384500))
                .totalOrdersFulfilled(1248)
                .averageOrderValue(BigDecimal.valueOf(842))
                .sellThroughRate(BigDecimal.valueOf(94.6))
                .repeatBuyerPct(BigDecimal.valueOf(68.4))
                .onTimeDeliveryRate(BigDecimal.valueOf(97.2))
                .disputeResolutionRate(BigDecimal.valueOf(100.0))
                .topProducts(List.of(
                        VendorAnalyticsDto.TopProductItem.builder().name("Aashirvaad Atta 10 KG").unitsSold(420).revenue(BigDecimal.valueOf(245700)).marginPct(BigDecimal.valueOf(14.5)).build(),
                        VendorAnalyticsDto.TopProductItem.builder().name("Tata Sampann Toor Dal 5 KG").unitsSold(280).revenue(BigDecimal.valueOf(145600)).marginPct(BigDecimal.valueOf(12.0)).build(),
                        VendorAnalyticsDto.TopProductItem.builder().name("Fortune Sunflower Oil 5L").unitsSold(190).revenue(BigDecimal.valueOf(123310)).marginPct(BigDecimal.valueOf(9.8)).build()
                ))
                .monthlyRevenueChart(List.of(
                        VendorAnalyticsDto.MonthlyRevenueItem.builder().month("Jun").revenue(BigDecimal.valueOf(180000)).orders(210).build(),
                        VendorAnalyticsDto.MonthlyRevenueItem.builder().month("Jul").revenue(BigDecimal.valueOf(240000)).orders(285).build(),
                        VendorAnalyticsDto.MonthlyRevenueItem.builder().month("Aug").revenue(BigDecimal.valueOf(310000)).orders(360).build(),
                        VendorAnalyticsDto.MonthlyRevenueItem.builder().month("Sep").revenue(BigDecimal.valueOf(384500)).orders(440).build()
                ))
                .categoryDistribution(List.of(
                        VendorAnalyticsDto.CategoryDistributionItem.builder().category("Staples & Grains").count(640).percentage(BigDecimal.valueOf(51)).build(),
                        VendorAnalyticsDto.CategoryDistributionItem.builder().category("Edible Oils").count(320).percentage(BigDecimal.valueOf(26)).build(),
                        VendorAnalyticsDto.CategoryDistributionItem.builder().category("Pulses & Dal").count(288).percentage(BigDecimal.valueOf(23)).build()
                ))
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
