package com.manacommunity.api.unit.service;

import com.manacommunity.api.fresh.processing.dto.*;
import com.manacommunity.api.fresh.processing.model.*;
import com.manacommunity.api.fresh.processing.repository.*;
import com.manacommunity.api.fresh.processing.service.FreshProcessingService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.vendor.commerce.repository.VendorProductRepository;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreshProcessingServiceTest {

    @Mock
    private FreshProcessingRecipeRepository recipeRepository;
    @Mock
    private FreshProcessingBatchRepository batchRepository;
    @Mock
    private FreshBatchQualityCheckRepository qualityCheckRepository;
    @Mock
    private FreshBatchReleaseRepository batchReleaseRepository;
    @Mock
    private FreshBatchTemperatureLogRepository temperatureLogRepository;
    @Mock
    private VmsVendorRepository vendorRepository;
    @Mock
    private VendorProductRepository vendorProductRepository;
    @Mock
    private AppUserRepository userRepository;

    @InjectMocks
    private FreshProcessingService processingService;

    private VmsVendor testVendor;
    private VendorProduct testProduct;
    private AppUser testUser;
    private FreshProcessingBatch testBatch;

    @BeforeEach
    void setUp() {
        testVendor = VmsVendor.builder().id(10L).businessName("Fresh Milling Co").build();
        testProduct = VendorProduct.builder().id(200L).name("Ragi Dosa Batter").build();
        testUser = new AppUser();
        testUser.setId(5L);
        testUser.setEmail("qc@fresh.com");

        testBatch = FreshProcessingBatch.builder()
                .id(1L)
                .batchNumber("BATCH-20261104-ABC123")
                .vendor(testVendor)
                .vendorProduct(testProduct)
                .productionDate(LocalDate.now())
                .rawMaterialInputQty(new BigDecimal("100.00"))
                .actualOutputQty(BigDecimal.ZERO)
                .actualYieldPercent(BigDecimal.ZERO)
                .wastageQty(BigDecimal.ZERO)
                .status(BatchStatus.IN_PRODUCTION)
                .expiryTimestamp(Instant.now().plusSeconds(86400))
                .build();
    }

    @Test
    void testCreateRecipe_Success() {
        when(vendorRepository.findById(10L)).thenReturn(Optional.of(testVendor));
        when(vendorProductRepository.findById(200L)).thenReturn(Optional.of(testProduct));
        when(recipeRepository.save(any(FreshProcessingRecipe.class))).thenAnswer(i -> {
            FreshProcessingRecipe r = i.getArgument(0);
            r.setId(100L);
            return r;
        });

        CreateRecipeRequest req = CreateRecipeRequest.builder()
                .vendorId(10L)
                .recipeCode("REC-RAGI-01")
                .recipeName("Standard Ragi Batter")
                .outputProductId(200L)
                .targetOutputUnit("LITRE")
                .expectedYieldPercent(new BigDecimal("95.00"))
                .shelfLifeHours(48)
                .build();

        RecipeDto result = processingService.createRecipe(req);

        assertNotNull(result);
        assertEquals("REC-RAGI-01", result.getRecipeCode());
        assertEquals("Standard Ragi Batter", result.getRecipeName());
        verify(recipeRepository, times(1)).save(any(FreshProcessingRecipe.class));
    }

    @Test
    void testRecordOutput_ComputesYieldAndMovesToQualityInspection() {
        when(batchRepository.findById(1L)).thenReturn(Optional.of(testBatch));
        when(batchRepository.save(any(FreshProcessingBatch.class))).thenAnswer(i -> i.getArgument(0));

        RecordOutputRequest req = RecordOutputRequest.builder()
                .actualOutputQty(new BigDecimal("92.00"))
                .wastageQty(new BigDecimal("8.00"))
                .wastageReason("Filter residue")
                .storageTempCelsius(new BigDecimal("3.5"))
                .build();

        BatchDto result = processingService.recordOutput(1L, req);

        assertNotNull(result);
        assertEquals(new BigDecimal("92.00"), result.getActualOutputQty());
        // 92 * 100 / 100 = 92.00%
        assertEquals(new BigDecimal("92.00"), result.getActualYieldPercent());
        assertEquals(BatchStatus.QUALITY_INSPECTION, result.getStatus());
        verify(batchRepository, times(1)).save(testBatch);
    }

    @Test
    void testAddQualityCheck_FailsCheck_QuarantinesBatch() {
        when(batchRepository.findById(1L)).thenReturn(Optional.of(testBatch));
        when(userRepository.findById(5L)).thenReturn(Optional.of(testUser));
        when(qualityCheckRepository.save(any(FreshBatchQualityCheck.class))).thenAnswer(i -> {
            FreshBatchQualityCheck q = i.getArgument(0);
            q.setId(10L);
            return q;
        });

        AddQualityCheckRequest req = AddQualityCheckRequest.builder()
                .checkName("PH_ACIDITY")
                .measuredValue("6.8")
                .isPassed(false)
                .checkedByUserId(5L)
                .notes("Acidity above threshold for fresh batter")
                .build();

        QualityCheckDto result = processingService.addQualityCheck(1L, req);

        assertNotNull(result);
        assertFalse(result.getIsPassed());
        assertEquals(BatchStatus.QUARANTINED, testBatch.getStatus());
        verify(batchRepository, times(1)).save(testBatch);
    }

    @Test
    void testReleaseBatch_Approved_SetsStatusReleased() {
        when(batchRepository.findById(1L)).thenReturn(Optional.of(testBatch));
        when(userRepository.findById(5L)).thenReturn(Optional.of(testUser));
        when(batchReleaseRepository.save(any(FreshBatchRelease.class))).thenAnswer(i -> {
            FreshBatchRelease r = i.getArgument(0);
            r.setId(50L);
            return r;
        });

        BatchReleaseRequest req = BatchReleaseRequest.builder()
                .releasedByUserId(5L)
                .releaseDecision(ReleaseDecision.APPROVED)
                .temperatureVerifiedCelsius(new BigDecimal("3.2"))
                .remarks("Cold-chain verified, ready for dispatch")
                .build();

        BatchReleaseDto result = processingService.releaseBatch(1L, req);

        assertNotNull(result);
        assertEquals(ReleaseDecision.APPROVED, result.getReleaseDecision());
        assertEquals(BatchStatus.RELEASED, testBatch.getStatus());
        verify(batchRepository, times(1)).save(testBatch);
    }
}