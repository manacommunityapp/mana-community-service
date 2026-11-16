package com.manacommunity.api.unit.service;

import com.manacommunity.api.fresh.catalog.dto.*;
import com.manacommunity.api.fresh.catalog.model.*;
import com.manacommunity.api.fresh.catalog.repository.*;
import com.manacommunity.api.fresh.catalog.service.FreshCatalogService;
import com.manacommunity.api.fresh.vendor.model.FreshVendorCapabilityType;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.vendor.commerce.repository.VendorProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreshCatalogServiceTest {

    @Mock
    private VendorProductRepository vendorProductRepository;
    @Mock
    private FreshProductAttributeRepository attributeRepository;
    @Mock
    private FreshProductNutritionRepository nutritionRepository;
    @Mock
    private FreshProductAllergenRepository allergenRepository;
    @Mock
    private FreshProductPricingRepository pricingRepository;
    @Mock
    private FreshProductCatalogProfileRepository catalogProfileRepository;

    @InjectMocks
    private FreshCatalogService catalogService;

    private VendorProduct testProduct;

    @BeforeEach
    void setUp() {
        testProduct = VendorProduct.builder()
                .id(101L)
                .name("Organic Idli Dosa Batter")
                .category("Fresh Foods")
                .build();
    }

    @Test
    void testSimulateAndSavePricing_CalculatesSellingPriceCorrectly() {
        when(vendorProductRepository.findById(101L)).thenReturn(Optional.of(testProduct));
        when(pricingRepository.findActiveByProductId(101L)).thenReturn(Optional.empty());
        when(pricingRepository.save(any(FreshProductPricing.class))).thenAnswer(invocation -> {
            FreshProductPricing saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        PricingSimulatorRequest request = PricingSimulatorRequest.builder()
                .vendorProductId(101L)
                .vendorCostPrice(new BigDecimal("100.00"))
                .manaCommissionPct(new BigDecimal("10.00"))
                .deliveryFee(new BigDecimal("5.00"))
                .packagingFee(new BigDecimal("5.00"))
                .gstRatePct(new BigDecimal("5.00"))
                .effectiveFrom(LocalDate.now())
                .build();

        FreshProductPricingDto result = catalogService.simulateAndSavePricing(request);

        assertNotNull(result);
        assertEquals(101L, result.getVendorProductId());
        // Cost: 100, Commission: 10, Delivery: 5, Packaging: 5 -> Base: 120. GST: 5% of 120 = 6. Total = 126.00
        assertEquals(new BigDecimal("126.00"), result.getResidentSellingPrice());
        assertFalse(result.getAdminApproved());
        verify(pricingRepository, times(1)).save(any(FreshProductPricing.class));
    }

    @Test
    void testSaveOrUpdateProfile_SavesProfileData() {
        when(vendorProductRepository.findById(101L)).thenReturn(Optional.of(testProduct));
        when(catalogProfileRepository.findByVendorProductId(101L)).thenReturn(Optional.empty());
        when(catalogProfileRepository.save(any(FreshProductCatalogProfile.class))).thenAnswer(i -> i.getArgument(0));

        FreshCatalogProfileRequest request = FreshCatalogProfileRequest.builder()
                .vendorProductId(101L)
                .capabilityType(FreshVendorCapabilityType.BATTER_MANUFACTURER)
                .shelfLifeHours(72)
                .storageInstruction("Keep refrigerated between 2C and 5C")
                .isOrganic(true)
                .orderUnit("litre")
                .minOrderQty(BigDecimal.ONE)
                .build();

        FreshProductCatalogDto result = catalogService.saveOrUpdateProfile(request);

        assertNotNull(result);
        assertEquals(101L, result.getVendorProductId());
        verify(catalogProfileRepository, times(1)).save(any(FreshProductCatalogProfile.class));
    }

    @Test
    void testSaveAttributes_ReplacesExistingAttributes() {
        when(vendorProductRepository.findById(101L)).thenReturn(Optional.of(testProduct));
        when(attributeRepository.findByVendorProductIdOrderByDisplayOrderAsc(101L))
                .thenReturn(Collections.emptyList());
        when(attributeRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        FreshProductAttributeRequest.AttributeItem item = new FreshProductAttributeRequest.AttributeItem();
        item.setAttributeKey("FERMENTATION_HOURS");
        item.setAttributeValue("12");
        item.setUnit("hours");
        item.setDisplayOrder(0);
        item.setIsSearchable(true);

        FreshProductAttributeRequest request = FreshProductAttributeRequest.builder()
                .attributes(List.of(item))
                .build();

        List<FreshProductAttributeDto> attrs = catalogService.saveAttributes(101L, request);

        assertEquals(1, attrs.size());
        assertEquals("FERMENTATION_HOURS", attrs.get(0).getAttributeKey());
        assertEquals("12", attrs.get(0).getAttributeValue());
        verify(attributeRepository, times(1)).deleteAll(any());
        verify(attributeRepository, times(1)).saveAll(any());
    }

    @Test
    void testApprovePricing_SetsAdminApprovedTrue() {
        FreshProductPricing pricing = FreshProductPricing.builder()
                .id(50L)
                .vendorProduct(testProduct)
                .vendorCostPrice(new BigDecimal("50.00"))
                .residentSellingPrice(new BigDecimal("65.00"))
                .adminApproved(false)
                .build();

        when(pricingRepository.findById(50L)).thenReturn(Optional.of(pricing));
        when(pricingRepository.save(any(FreshProductPricing.class))).thenAnswer(i -> i.getArgument(0));

        FreshProductPricingDto result = catalogService.approvePricing(50L, 99L);

        assertTrue(result.getAdminApproved());
        assertNotNull(result.getAdminApprovedAt());
    }
}