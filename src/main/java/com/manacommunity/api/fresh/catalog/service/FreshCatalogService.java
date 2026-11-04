package com.manacommunity.api.fresh.catalog.service;

import com.manacommunity.api.fresh.catalog.dto.*;
import com.manacommunity.api.fresh.catalog.model.*;
import com.manacommunity.api.fresh.catalog.repository.*;
import com.manacommunity.api.fresh.vendor.model.FreshVendorCapabilityType;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.vendor.commerce.repository.VendorProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FreshCatalogService {

    private final VendorProductRepository vendorProductRepository;
    private final FreshProductAttributeRepository attributeRepository;
    private final FreshProductNutritionRepository nutritionRepository;
    private final FreshProductAllergenRepository allergenRepository;
    private final FreshProductPricingRepository pricingRepository;
    private final FreshProductCatalogProfileRepository catalogProfileRepository;

    // ---- Catalog Profile ----

    @Transactional
    public FreshProductCatalogDto saveOrUpdateProfile(FreshCatalogProfileRequest request) {
        VendorProduct product = vendorProductRepository.findById(request.getVendorProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.getVendorProductId()));

        FreshProductCatalogProfile profile = catalogProfileRepository
                .findByVendorProductId(product.getId())
                .orElse(FreshProductCatalogProfile.builder().vendorProduct(product).build());

        profile.setCapabilityType(request.getCapabilityType());
        profile.setShelfLifeHours(request.getShelfLifeHours());
        profile.setStorageInstruction(request.getStorageInstruction());
        profile.setOriginState(request.getOriginState());
        profile.setOriginDistrict(request.getOriginDistrict());
        profile.setIsOrganic(Boolean.TRUE.equals(request.getIsOrganic()));
        profile.setOrganicCertNumber(request.getOrganicCertNumber());
        profile.setIsPesticideFree(Boolean.TRUE.equals(request.getIsPesticideFree()));
        profile.setIsAntibioticFree(Boolean.TRUE.equals(request.getIsAntibioticFree()));
        profile.setIsHormoneFree(Boolean.TRUE.equals(request.getIsHormoneFree()));
        if (request.getMinOrderQty() != null) profile.setMinOrderQty(request.getMinOrderQty());
        profile.setMaxOrderQty(request.getMaxOrderQty());
        if (request.getOrderUnit() != null) profile.setOrderUnit(request.getOrderUnit());

        catalogProfileRepository.save(profile);
        return buildCatalogDto(product.getId());
    }

    public FreshProductCatalogDto getCatalog(Long vendorProductId) {
        return buildCatalogDto(vendorProductId);
    }

    // ---- Attributes ----

    @Transactional
    public List<FreshProductAttributeDto> saveAttributes(Long vendorProductId,
                                                          FreshProductAttributeRequest request) {
        VendorProduct product = vendorProductRepository.findById(vendorProductId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        // Replace all attributes for this product
        attributeRepository.deleteAll(attributeRepository.findByVendorProductIdOrderByDisplayOrderAsc(vendorProductId));
        List<FreshProductAttribute> saved = request.getAttributes().stream()
                .map(a -> FreshProductAttribute.builder()
                        .vendorProduct(product)
                        .attributeKey(a.getAttributeKey())
                        .attributeValue(a.getAttributeValue())
                        .unit(a.getUnit())
                        .displayOrder(a.getDisplayOrder() != null ? a.getDisplayOrder() : 0)
                        .isSearchable(Boolean.TRUE.equals(a.getIsSearchable()))
                        .build())
                .collect(Collectors.toList());
        attributeRepository.saveAll(saved);
        return saved.stream().map(this::toAttributeDto).collect(Collectors.toList());
    }

    public List<FreshProductAttributeDto> getAttributes(Long vendorProductId) {
        return attributeRepository.findByVendorProductIdOrderByDisplayOrderAsc(vendorProductId)
                .stream().map(this::toAttributeDto).collect(Collectors.toList());
    }

    // ---- Nutrition ----

    @Transactional
    public FreshProductNutritionDto saveOrUpdateNutrition(FreshProductNutritionDto dto) {
        VendorProduct product = vendorProductRepository.findById(dto.getVendorProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        FreshProductNutrition nutrition = nutritionRepository
                .findByVendorProductId(dto.getVendorProductId())
                .orElse(FreshProductNutrition.builder().vendorProduct(product).build());

        nutrition.setServingSizeGrams(dto.getServingSizeGrams());
        nutrition.setCaloriesKcal(dto.getCaloriesKcal());
        nutrition.setProteinG(dto.getProteinG());
        nutrition.setCarbohydratesG(dto.getCarbohydratesG());
        nutrition.setFatG(dto.getFatG());
        nutrition.setSaturatedFatG(dto.getSaturatedFatG());
        nutrition.setFibreG(dto.getFibreG());
        nutrition.setSodiumMg(dto.getSodiumMg());
        nutrition.setSugarG(dto.getSugarG());
        nutrition.setMicronutrients(dto.getMicronutrients());
        nutrition.setDietitianCertified(Boolean.TRUE.equals(dto.getDietitianCertified()));
        nutrition.setDietitianName(dto.getDietitianName());
        nutrition.setCertificationNumber(dto.getCertificationNumber());
        nutrition.setCertifiedOn(dto.getCertifiedOn());
        nutrition.setFssaiLabelCompliant(Boolean.TRUE.equals(dto.getFssaiLabelCompliant()));
        nutritionRepository.save(nutrition);
        return toNutritionDto(nutrition);
    }

    // ---- Allergens ----

    @Transactional
    public List<FreshProductAllergenDto> saveAllergens(Long vendorProductId,
                                                        List<FreshProductAllergenDto> dtos) {
        VendorProduct product = vendorProductRepository.findById(vendorProductId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        allergenRepository.deleteAll(allergenRepository.findByVendorProductId(vendorProductId));
        List<FreshProductAllergen> saved = dtos.stream()
                .map(d -> FreshProductAllergen.builder()
                        .vendorProduct(product)
                        .allergenName(d.getAllergenName())
                        .severity(d.getSeverity() != null ? d.getSeverity() : FreshProductAllergen.Severity.CONTAINS)
                        .build())
                .collect(Collectors.toList());
        allergenRepository.saveAll(saved);
        return saved.stream().map(this::toAllergenDto).collect(Collectors.toList());
    }

    // ---- Pricing ----

    @Transactional
    public FreshProductPricingDto simulateAndSavePricing(PricingSimulatorRequest request) {
        VendorProduct product = vendorProductRepository.findById(request.getVendorProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        BigDecimal commPct = request.getManaCommissionPct() != null ? request.getManaCommissionPct() : BigDecimal.valueOf(10);
        BigDecimal delivery = request.getDeliveryFee() != null ? request.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal packaging = request.getPackagingFee() != null ? request.getPackagingFee() : BigDecimal.ZERO;
        BigDecimal gst = request.getGstRatePct() != null ? request.getGstRatePct() : BigDecimal.valueOf(5);

        BigDecimal sellingPrice = FreshProductPricing.computeSellingPrice(
                request.getVendorCostPrice(), commPct, delivery, packaging, gst);

        // Deactivate previous active pricing
        pricingRepository.findActiveByProductId(product.getId())
                .ifPresent(prev -> { prev.setIsActive(false); pricingRepository.save(prev); });

        FreshProductPricing pricing = FreshProductPricing.builder()
                .vendorProduct(product)
                .effectiveFrom(request.getEffectiveFrom() != null ? request.getEffectiveFrom() : LocalDate.now())
                .effectiveTo(request.getEffectiveTo())
                .vendorCostPrice(request.getVendorCostPrice())
                .manaCommissionPct(commPct)
                .deliveryFee(delivery)
                .packagingFee(packaging)
                .gstRatePct(gst)
                .residentSellingPrice(sellingPrice)
                .isActive(true)
                .adminApproved(false)
                .build();
        pricingRepository.save(pricing);
        return toPricingDto(pricing);
    }

    public FreshProductPricingDto getActivePricing(Long vendorProductId) {
        return pricingRepository.findActiveByProductId(vendorProductId)
                .map(this::toPricingDto)
                .orElse(null);
    }

    public List<FreshProductPricingDto> getPendingAdminApproval() {
        return pricingRepository.findPendingAdminApproval().stream()
                .map(this::toPricingDto).collect(Collectors.toList());
    }

    @Transactional
    public FreshProductPricingDto approvePricing(Long pricingId, Long approvedByUserId) {
        FreshProductPricing pricing = pricingRepository.findById(pricingId)
                .orElseThrow(() -> new IllegalArgumentException("Pricing record not found: " + pricingId));
        pricing.setAdminApproved(true);
        pricing.setAdminApprovedAt(java.time.Instant.now());
        pricingRepository.save(pricing);
        return toPricingDto(pricing);
    }

    // ---- Private builders ----

    private FreshProductCatalogDto buildCatalogDto(Long vendorProductId) {
        FreshProductCatalogProfile profile = catalogProfileRepository.findByVendorProductId(vendorProductId).orElse(null);
        FreshProductNutrition nutrition = nutritionRepository.findByVendorProductId(vendorProductId).orElse(null);
        List<FreshProductAttribute> attrs = attributeRepository.findByVendorProductIdOrderByDisplayOrderAsc(vendorProductId);
        List<FreshProductAllergen> allergens = allergenRepository.findByVendorProductId(vendorProductId);
        FreshProductPricingDto activePricing = pricingRepository.findActiveByProductId(vendorProductId)
                .map(this::toPricingDto).orElse(null);

        FreshVendorCapabilityType capType = profile != null ? profile.getCapabilityType() : null;
        String storageInstruction = profile != null ? profile.getStorageInstruction() : null;
        String originState = profile != null ? profile.getOriginState() : null;
        Boolean isOrganic = profile != null ? profile.getIsOrganic() : null;
        Boolean isPesticideFree = profile != null ? profile.getIsPesticideFree() : null;
        Boolean isAntibioticFree = profile != null ? profile.getIsAntibioticFree() : null;
        Boolean isHormoneFree = profile != null ? profile.getIsHormoneFree() : null;
        java.math.BigDecimal minQty = profile != null ? profile.getMinOrderQty() : null;
        java.math.BigDecimal maxQty = profile != null ? profile.getMaxOrderQty() : null;
        String orderUnit = profile != null ? profile.getOrderUnit() : null;
        Integer shelfLife = profile != null ? profile.getShelfLifeHours() : null;

        return FreshProductCatalogDto.builder()
                .vendorProductId(vendorProductId)
                .capabilityType(capType)
                .shelfLifeHours(shelfLife)
                .storageInstruction(storageInstruction)
                .originState(originState)
                .isOrganic(isOrganic)
                .isPesticideFree(isPesticideFree)
                .isAntibioticFree(isAntibioticFree)
                .isHormoneFree(isHormoneFree)
                .minOrderQty(minQty)
                .maxOrderQty(maxQty)
                .orderUnit(orderUnit)
                .activePricing(activePricing)
                .attributes(attrs.stream().map(this::toAttributeDto).collect(Collectors.toList()))
                .nutrition(nutrition != null ? toNutritionDto(nutrition) : null)
                .allergens(allergens.stream().map(this::toAllergenDto).collect(Collectors.toList()))
                .build();
    }

    private FreshProductAttributeDto toAttributeDto(FreshProductAttribute a) {
        return FreshProductAttributeDto.builder()
                .id(a.getId()).attributeKey(a.getAttributeKey())
                .attributeValue(a.getAttributeValue()).unit(a.getUnit())
                .displayOrder(a.getDisplayOrder()).isSearchable(a.getIsSearchable())
                .createdAt(a.getCreatedAt()).build();
    }

    private FreshProductNutritionDto toNutritionDto(FreshProductNutrition n) {
        return FreshProductNutritionDto.builder()
                .id(n.getId()).vendorProductId(n.getVendorProduct().getId())
                .servingSizeGrams(n.getServingSizeGrams()).caloriesKcal(n.getCaloriesKcal())
                .proteinG(n.getProteinG()).carbohydratesG(n.getCarbohydratesG())
                .fatG(n.getFatG()).saturatedFatG(n.getSaturatedFatG())
                .fibreG(n.getFibreG()).sodiumMg(n.getSodiumMg()).sugarG(n.getSugarG())
                .micronutrients(n.getMicronutrients()).dietitianCertified(n.getDietitianCertified())
                .dietitianName(n.getDietitianName()).certificationNumber(n.getCertificationNumber())
                .certifiedOn(n.getCertifiedOn()).fssaiLabelCompliant(n.getFssaiLabelCompliant())
                .build();
    }

    private FreshProductAllergenDto toAllergenDto(FreshProductAllergen a) {
        return FreshProductAllergenDto.builder()
                .id(a.getId()).allergenName(a.getAllergenName()).severity(a.getSeverity()).build();
    }

    private FreshProductPricingDto toPricingDto(FreshProductPricing p) {
        return FreshProductPricingDto.builder()
                .id(p.getId()).vendorProductId(p.getVendorProduct().getId())
                .productName(p.getVendorProduct().getName())
                .effectiveFrom(p.getEffectiveFrom()).effectiveTo(p.getEffectiveTo())
                .vendorCostPrice(p.getVendorCostPrice()).manaCommissionPct(p.getManaCommissionPct())
                .deliveryFee(p.getDeliveryFee()).packagingFee(p.getPackagingFee())
                .gstRatePct(p.getGstRatePct()).residentSellingPrice(p.getResidentSellingPrice())
                .isActive(p.getIsActive()).adminApproved(p.getAdminApproved())
                .adminApprovedAt(p.getAdminApprovedAt())
                .build();
    }
}