package com.manacommunity.api.inventory;

import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.service.InventoryPickingService;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Inventory Concurrency and Non-Negative Stock Ledger Tests")
class InventoryConcurrencyTest {

    @Mock
    private InventoryPickingRepository pickingRepo;
    @Mock
    private InventoryPickingTypeRepository pickingTypeRepo;
    @Mock
    private InventoryMoveLineRepository moveLineRepo;
    @Mock
    private InventoryProductRepository productRepo;
    @Mock
    private InventoryLocationRepository locationRepo;
    @Mock
    private InventoryStockQuantRepository quantRepo;
    @Mock
    private InventorySequenceRepository seqRepo;
    @Mock
    private InventoryLotRepository lotRepo;
    @Mock
    private StockLedgerEntryRepository ledgerRepo;
    @Mock
    private AuditService auditService;
    @Mock
    private LoggedInUserService loggedInUserService;

    @InjectMocks
    private InventoryPickingService pickingService;

    private InventoryProduct product;
    private InventoryLocation internalLocation;
    private InventoryLocation scrapLocation;
    private InventoryStockQuant quant;
    private InventoryStockQuant scrapQuant;

    @BeforeEach
    void setUp() {
        product = InventoryProduct.builder()
                .id(1L)
                .name("Standard Steel Bolts (M8)")
                .defaultCode("BOLT-M8")
                .standardPrice(BigDecimal.valueOf(15.00))
                .build();

        internalLocation = InventoryLocation.builder()
                .id(10L)
                .completeName("WH/Stock/Shelf A-1")
                .usage(InventoryLocation.LocationUsage.INTERNAL)
                .build();

        scrapLocation = InventoryLocation.builder()
                .id(20L)
                .completeName("WH/Scrap")
                .usage(InventoryLocation.LocationUsage.SCRAP)
                .build();

        quant = InventoryStockQuant.builder()
                .id(100L)
                .product(product)
                .location(internalLocation)
                .quantity(BigDecimal.valueOf(10))
                .reservedQty(BigDecimal.valueOf(2))
                .build();

        scrapQuant = InventoryStockQuant.builder()
                .id(200L)
                .product(product)
                .location(scrapLocation)
                .quantity(BigDecimal.ZERO)
                .reservedQty(BigDecimal.ZERO)
                .build();
    }

    @Test
    @DisplayName("Negative Stock Prevention: Deducting more than on-hand stock must throw IllegalStateException")
    void testNegativeStockRejection() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepo.findById(10L)).thenReturn(Optional.of(internalLocation));
        when(locationRepo.findByUsageAndIsActiveTrueOrderByCompleteNameAsc(InventoryLocation.LocationUsage.SCRAP))
                .thenReturn(List.of(scrapLocation));
        when(quantRepo.findForUpdateByProductIdAndLocationId(1L, 10L)).thenReturn(Optional.of(quant));

        // Attempting to scrap/deduct 15 units when only 10 exist
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            pickingService.scrapStock(1L, 10L, BigDecimal.valueOf(15), "Damaged in transit");
        });

        assertTrue(ex.getMessage().contains("Insufficient stock for product"));
        verify(quantRepo, never()).save(any());
        verify(ledgerRepo, never()).save(any());
    }

    @Test
    @DisplayName("Reserved Stock Violation: Deducting below reserved quantity must be rejected")
    void testReservedStockViolationRejection() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepo.findById(10L)).thenReturn(Optional.of(internalLocation));
        when(locationRepo.findByUsageAndIsActiveTrueOrderByCompleteNameAsc(InventoryLocation.LocationUsage.SCRAP))
                .thenReturn(List.of(scrapLocation));
        when(quantRepo.findForUpdateByProductIdAndLocationId(1L, 10L)).thenReturn(Optional.of(quant));

        // On-hand: 10, Reserved: 2. Attempting to deduct 9 units leaves 1, which is < reserved (2)
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            pickingService.scrapStock(1L, 10L, BigDecimal.valueOf(9), "Corrosion inspection");
        });

        assertTrue(ex.getMessage().contains("would violate reserved quantity"));
        verify(quantRepo, never()).save(any());
        verify(ledgerRepo, never()).save(any());
    }

    @Test
    @DisplayName("Authoritative Stock Ledger Recording: Valid deduction updates quant and appends immutable ledger entry")
    void testSuccessfulDeductionAndLedgerAudit() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepo.findById(10L)).thenReturn(Optional.of(internalLocation));
        when(locationRepo.findByUsageAndIsActiveTrueOrderByCompleteNameAsc(InventoryLocation.LocationUsage.SCRAP))
                .thenReturn(List.of(scrapLocation));
        when(quantRepo.findForUpdateByProductIdAndLocationId(1L, 10L)).thenReturn(Optional.of(quant));
        when(quantRepo.findForUpdateByProductIdAndLocationId(1L, 20L)).thenReturn(Optional.of(scrapQuant));

        // Deduct 5 units (10 - 5 = 5 >= 2 reserved)
        pickingService.scrapStock(1L, 10L, BigDecimal.valueOf(5), "Batch expiry test");

        // Assert source quant updated
        assertEquals(0, BigDecimal.valueOf(5).compareTo(quant.getQuantity()));
        verify(quantRepo, times(1)).save(quant);
        // Assert scrap quant updated
        assertEquals(0, BigDecimal.valueOf(5).compareTo(scrapQuant.getQuantity()));
        verify(quantRepo, times(1)).save(scrapQuant);

        // Assert immutable ledger entries created (one debit, one credit in double-entry)
        verify(ledgerRepo, times(2)).save(any(StockLedgerEntry.class));
    }
}
