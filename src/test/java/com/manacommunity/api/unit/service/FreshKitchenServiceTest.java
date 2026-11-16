package com.manacommunity.api.unit.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.freshkitchen.dto.*;
import com.manacommunity.api.freshkitchen.model.*;
import com.manacommunity.api.freshkitchen.repository.BreakfastComboItemRepository;
import com.manacommunity.api.freshkitchen.repository.BreakfastComboRepository;
import com.manacommunity.api.freshkitchen.repository.FoodProductionBatchRepository;
import com.manacommunity.api.freshkitchen.repository.FreshFoodProductConfigRepository;
import com.manacommunity.api.freshkitchen.service.FreshKitchenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreshKitchenServiceTest {

    @Mock
    private FreshFoodProductConfigRepository productConfigRepository;
    @Mock
    private BreakfastComboRepository comboRepository;
    @Mock
    private BreakfastComboItemRepository comboItemRepository;
    @Mock
    private FoodProductionBatchRepository batchRepository;

    @InjectMocks
    private FreshKitchenService freshKitchenService;

    private FreshFoodProductConfig idliConfig;
    private FreshFoodProductConfig ragiConfig;
    private BreakfastCombo testCombo;
    private BreakfastComboItem mainItem;
    private FoodProductionBatch testBatch;

    @BeforeEach
    void setUp() {
        idliConfig = FreshFoodProductConfig.builder()
                .id(1L)
                .productId("BAT-IDLI-1KG")
                .name("Fresh Idli Batter")
                .category(FoodCategory.BATTER)
                .unitPrice(new BigDecimal("65.00"))
                .shelfLifeHours(48)
                .storageTempMin(new BigDecimal("2.0"))
                .storageTempMax(new BigDecimal("4.0"))
                .isActive(true)
                .build();

        ragiConfig = FreshFoodProductConfig.builder()
                .id(2L)
                .productId("BAT-RAGI-1KG")
                .name("Organic Ragi Batter")
                .category(FoodCategory.BATTER)
                .unitPrice(new BigDecimal("80.00"))
                .shelfLifeHours(48)
                .storageTempMin(new BigDecimal("2.0"))
                .storageTempMax(new BigDecimal("4.0"))
                .isActive(true)
                .build();

        mainItem = BreakfastComboItem.builder()
                .id(10L)
                .productId("BAT-IDLI-1KG")
                .productName("Fresh Idli Batter")
                .itemRole(ComboItemRole.MAIN)
                .defaultQuantity(1)
                .unitPrice(new BigDecimal("65.00"))
                .isMandatory(true)
                .isCustomizable(true)
                .allowedSubstitutes("BAT-RAGI-1KG")
                .build();

        testCombo = BreakfastCombo.builder()
                .id(100L)
                .comboCode("CMB-TEST")
                .name("South Indian Breakfast Combo")
                .basePrice(new BigDecimal("150.00"))
                .discountedPrice(new BigDecimal("135.00"))
                .isActive(true)
                .items(List.of(mainItem))
                .build();

        testBatch = FoodProductionBatch.builder()
                .id(50L)
                .batchNumber("BATCH-IDLI-20261103-01")
                .productId("BAT-IDLI-1KG")
                .productName("Fresh Idli Batter")
                .vendorId("VND-01")
                .productionDate(LocalDate.now())
                .batchQuantity(200)
                .batchStatus(BatchStatus.PLANNED)
                .expiryDateTime(LocalDateTime.now().plusHours(48))
                .build();
    }

    @Test
    void testGetBatters_Success() {
        when(productConfigRepository.findByCategoryAndIsActiveTrue(FoodCategory.BATTER))
                .thenReturn(List.of(idliConfig, ragiConfig));

        List<FreshProductDto> batters = freshKitchenService.getBatters();

        assertEquals(2, batters.size());
        assertEquals("BAT-IDLI-1KG", batters.get(0).getProductId());
        verify(productConfigRepository).findByCategoryAndIsActiveTrue(FoodCategory.BATTER);
    }

    @Test
    void testCustomizeCombo_WithSubstitute_PriceAdjusted() {
        when(comboRepository.findByComboCodeAndIsActiveTrue("CMB-TEST"))
                .thenReturn(Optional.of(testCombo));
        when(productConfigRepository.findByProductId("BAT-RAGI-1KG"))
                .thenReturn(Optional.of(ragiConfig));

        CustomizeComboRequest request = CustomizeComboRequest.builder()
                .comboCode("CMB-TEST")
                .selectedItems(List.of(
                        CustomizeComboRequest.CustomizedItemSelection.builder()
                                .originalProductId("BAT-IDLI-1KG")
                                .selectedProductId("BAT-RAGI-1KG")
                                .quantity(1)
                                .build()
                ))
                .build();

        CustomizedComboResponseDto response = freshKitchenService.customizeCombo(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("135.00"), response.getBasePrice());
        // Ragi (80.00) - Idli (65.00) = +15.00 adjustment
        assertEquals(new BigDecimal("15.00"), response.getPriceAdjustment());
        assertEquals(new BigDecimal("150.00"), response.getFinalPrice());
        assertEquals(1, response.getItemBreakdown().size());
        assertEquals("BAT-RAGI-1KG", response.getItemBreakdown().get(0).getSelectedProductId());
    }

    @Test
    void testReleaseBatch_ValidColdChainTemperature_StatusReleased() {
        when(batchRepository.findByBatchNumber("BATCH-IDLI-20261103-01"))
                .thenReturn(Optional.of(testBatch));
        when(productConfigRepository.findByProductId("BAT-IDLI-1KG"))
                .thenReturn(Optional.of(idliConfig));
        when(batchRepository.save(any(FoodProductionBatch.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        BatchReleaseRequest request = BatchReleaseRequest.builder()
                .storageTempCelsius(new BigDecimal("3.2"))
                .releasedBy("QA_CHIEF")
                .notes("Hygienic cold chain validated")
                .build();

        ProductionBatchDto released = freshKitchenService.releaseBatch("BATCH-IDLI-20261103-01", request);

        assertNotNull(released);
        assertEquals(BatchStatus.RELEASED, released.getBatchStatus());
        assertEquals(new BigDecimal("3.2"), released.getStorageTempCelsius());
        assertEquals("QA_CHIEF", released.getReleasedBy());
    }

    @Test
    void testReleaseBatch_TemperatureExceeded_ThrowsAndMarksRejected() {
        when(batchRepository.findByBatchNumber("BATCH-IDLI-20261103-01"))
                .thenReturn(Optional.of(testBatch));
        when(productConfigRepository.findByProductId("BAT-IDLI-1KG"))
                .thenReturn(Optional.of(idliConfig));
        when(batchRepository.save(any(FoodProductionBatch.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 6.5°C violates the 2.0°C - 4.0°C rule
        BatchReleaseRequest request = BatchReleaseRequest.builder()
                .storageTempCelsius(new BigDecimal("6.5"))
                .releasedBy("QA_INSPECTOR")
                .build();

        assertThrows(InvalidInputException.class, () ->
                freshKitchenService.releaseBatch("BATCH-IDLI-20261103-01", request));

        assertEquals(BatchStatus.REJECTED, testBatch.getBatchStatus());
        verify(batchRepository).save(testBatch);
    }

    @Test
    void testExpireOverdueBatches_LocksOutPerishables() {
        FoodProductionBatch expiredBatch = FoodProductionBatch.builder()
                .id(99L)
                .batchNumber("BATCH-OLD-01")
                .batchStatus(BatchStatus.RELEASED)
                .expiryDateTime(LocalDateTime.now().minusHours(2))
                .build();

        when(batchRepository.findByBatchStatusAndExpiryDateTimeBefore(eq(BatchStatus.RELEASED), any(LocalDateTime.class)))
                .thenReturn(List.of(expiredBatch));

        int count = freshKitchenService.expireOverdueBatches();

        assertEquals(1, count);
        assertEquals(BatchStatus.EXPIRED, expiredBatch.getBatchStatus());
        verify(batchRepository).saveAll(any());
    }
}
