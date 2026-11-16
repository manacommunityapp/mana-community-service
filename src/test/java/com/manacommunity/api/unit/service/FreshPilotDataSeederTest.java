package com.manacommunity.api.unit.service;

import com.manacommunity.api.fresh.catalog.repository.*;
import com.manacommunity.api.fresh.dispatch.model.*;
import com.manacommunity.api.fresh.dispatch.repository.*;
import com.manacommunity.api.fresh.pilot.dto.FreshPilotMetricsDto;
import com.manacommunity.api.fresh.pilot.service.FreshPilotDataSeeder;
import com.manacommunity.api.fresh.processing.repository.*;
import com.manacommunity.api.fresh.vendor.repository.*;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.vendor.commerce.repository.VendorProductRepository;
import com.manacommunity.api.vendor.commerce.repository.VendorProductVariantRepository;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreshPilotDataSeederTest {

    @Mock private CommunityRepository communityRepository;
    @Mock private AppUserRepository userRepository;
    @Mock private VmsVendorRepository vendorRepository;
    @Mock private VendorProductRepository productRepository;
    @Mock private VendorProductVariantRepository variantRepository;
    @Mock private FreshVendorCapabilityRepository capabilityRepository;
    @Mock private FreshVendorStaffRepository staffRepository;
    @Mock private FreshProductCatalogProfileRepository catalogProfileRepository;
    @Mock private FreshProductAttributeRepository attributeRepository;
    @Mock private FreshProductNutritionRepository nutritionRepository;
    @Mock private FreshProductAllergenRepository allergenRepository;
    @Mock private FreshProductPricingRepository pricingRepository;
    @Mock private FreshProcessingRecipeRepository recipeRepository;
    @Mock private FreshProcessingBatchRepository batchRepository;
    @Mock private FreshBatchQualityCheckRepository qualityCheckRepository;
    @Mock private FreshBatchReleaseRepository releaseRepository;
    @Mock private FreshBatchTemperatureLogRepository temperatureLogRepository;
    @Mock private FreshDeliveryBatchRepository deliveryBatchRepository;
    @Mock private FreshDeliveryItemRepository deliveryItemRepository;
    @Mock private FreshVendorSettlementRepository settlementRepository;

    @InjectMocks
    private FreshPilotDataSeeder pilotDataSeeder;

    private Community testCommunity;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        testCommunity = Community.builder().id(1L).name("Prestige Lakeside Habitat").inviteCode("PLH-BLR-01").build();
        testUser = AppUser.builder().id(101L).email("fresh.pilot@manacommunity.com").fullName("Pilot Lead").build();
    }

    @Test
    void testSeedStage12PilotData_SuccessfullyInitializes() {
        when(communityRepository.findByInviteCode(anyString())).thenReturn(Optional.of(testCommunity));
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(vendorRepository.findAll()).thenReturn(Collections.emptyList());
        when(vendorRepository.save(any(VmsVendor.class))).thenAnswer(invocation -> {
            VmsVendor v = invocation.getArgument(0);
            v.setId(500L);
            return v;
        });
        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(variantRepository.findBySku(anyString())).thenReturn(Optional.empty());
        when(variantRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(recipeRepository.findByRecipeCode(anyString())).thenReturn(Optional.empty());
        when(recipeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(batchRepository.findByBatchNumber(anyString())).thenReturn(Optional.empty());
        when(batchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryBatchRepository.findByBatchCode(anyString())).thenReturn(Optional.empty());
        when(deliveryBatchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = pilotDataSeeder.seedStage12PilotData();

        assertNotNull(result);
        assertEquals("SUCCESS", result.get("status"));
        assertEquals(5, result.get("pilotVendorsCount"));
        assertEquals("RUN-20261104-PL01", result.get("deliveryRun"));
        assertEquals("SETTL-PILOT-2026-W44", result.get("settlementRef"));

        verify(vendorRepository, atLeast(5)).save(any(VmsVendor.class));
        verify(capabilityRepository, atLeast(5)).save(any());
        verify(staffRepository, atLeast(5)).save(any());
    }

    @Test
    void testGetPilotMetrics_AggregatesAccurately() {
        when(vendorRepository.count()).thenReturn(5L);
        when(capabilityRepository.count()).thenReturn(6L);
        when(batchRepository.count()).thenReturn(2L);
        when(releaseRepository.count()).thenReturn(1L);

        FreshDeliveryItem item1 = FreshDeliveryItem.builder().id(1L).handoverStatus(HandoverStatus.DELIVERED).build();
        FreshDeliveryItem item2 = FreshDeliveryItem.builder().id(2L).handoverStatus(HandoverStatus.PENDING).build();
        when(deliveryItemRepository.findAll()).thenReturn(List.of(item1, item2));

        FreshVendorSettlement settlement = FreshVendorSettlement.builder()
                .id(1L)
                .status(SettlementStatus.PROCESSED)
                .netPayableAmount(new BigDecimal("13050.00"))
                .build();
        when(settlementRepository.findAll()).thenReturn(List.of(settlement));
        when(communityRepository.findAll()).thenReturn(List.of(testCommunity));

        FreshPilotMetricsDto metrics = pilotDataSeeder.getPilotMetrics();

        assertNotNull(metrics);
        assertEquals(5, metrics.getTotalPilotVendors());
        assertEquals(6, metrics.getApprovedCapabilitiesCount());
        assertEquals(2, metrics.getActiveBatchesCount());
        assertEquals(1, metrics.getReleasedBatchesCount());
        assertEquals(50.0, metrics.getMorningDeliveryOnTimePercent());
        assertEquals(1, metrics.getCompletedDropsCount());
        assertEquals(1, metrics.getPendingDropsCount());
        assertEquals(new BigDecimal("13050.00"), metrics.getTotalSettledPayouts());
        assertEquals(1, metrics.getActivePilotCommunitiesCount());
    }
}
