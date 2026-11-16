package com.manacommunity.api.unit.service;

import com.manacommunity.api.fresh.city.model.FreshFulfilmentLocation;
import com.manacommunity.api.fresh.city.repository.FreshFulfilmentLocationRepository;
import com.manacommunity.api.fresh.inventory.dto.*;
import com.manacommunity.api.fresh.inventory.model.*;
import com.manacommunity.api.fresh.inventory.repository.FreshProductWastageRepository;
import com.manacommunity.api.fresh.inventory.repository.FreshStockMovementRepository;
import com.manacommunity.api.fresh.inventory.service.FreshStockLedgerService;
import com.manacommunity.api.freshkitchen.repository.FoodProductionBatchRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.vendor.commerce.repository.VendorProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreshStockLedgerServiceTest {

    @Mock
    private FreshStockMovementRepository movementRepository;
    @Mock
    private FreshProductWastageRepository wastageRepository;
    @Mock
    private VendorProductRepository vendorProductRepository;
    @Mock
    private FoodProductionBatchRepository batchRepository;
    @Mock
    private FreshFulfilmentLocationRepository locationRepository;
    @Mock
    private AppUserRepository userRepository;

    @InjectMocks
    private FreshStockLedgerService stockLedgerService;

    private VendorProduct product;
    private AppUser operator;

    @BeforeEach
    void setUp() {
        product = VendorProduct.builder()
                .id(100L)
                .name("Shimla Royal Apple")
                .build();

        operator = AppUser.builder().id(5L).fullName("Hub Inventory Clerk").build();
    }

    @Test
    @DisplayName("recordStockMovement inward receipt increases running balance")
    void testInwardReceiptIncreasesBalance() {
        when(vendorProductRepository.findById(100L)).thenReturn(Optional.of(product));
        when(movementRepository.findFirstByVendorProductIdOrderByCreatedAtDesc(100L)).thenReturn(Optional.empty());
        when(movementRepository.save(any(FreshStockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordStockMovementRequest req = RecordStockMovementRequest.builder()
                .vendorProductId(100L)
                .movementType(StockMovementType.INWARD_RECEIPT)
                .quantity(new BigDecimal("50.000"))
                .unitOfMeasure("KG")
                .referenceType("PURCHASE_ORDER")
                .referenceId("PO-202611-001")
                .build();

        StockMovementDto result = stockLedgerService.recordStockMovement(req);

        assertThat(result.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("50.000"));
        assertThat(result.getMovementType()).isEqualTo(StockMovementType.INWARD_RECEIPT);
        verify(movementRepository).save(any(FreshStockMovement.class));
    }

    @Test
    @DisplayName("recordWastage records loss and automatically deducts from stock ledger")
    void testRecordWastageAndLedgerDeduction() {
        when(vendorProductRepository.findById(100L)).thenReturn(Optional.of(product));
        when(userRepository.findById(5L)).thenReturn(Optional.of(operator));

        // Prior balance of 50.000 kg
        FreshStockMovement prev = FreshStockMovement.builder()
                .vendorProduct(product)
                .balanceAfter(new BigDecimal("50.000"))
                .build();

        when(movementRepository.findFirstByVendorProductIdOrderByCreatedAtDesc(100L)).thenReturn(Optional.of(prev));
        when(wastageRepository.save(any(FreshProductWastage.class))).thenAnswer(inv -> {
            FreshProductWastage w = inv.getArgument(0);
            w.setId(11L);
            return w;
        });
        when(movementRepository.save(any(FreshStockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordWastageRequest req = RecordWastageRequest.builder()
                .vendorProductId(100L)
                .quantity(new BigDecimal("5.000"))
                .unitOfMeasure("KG")
                .costPricePerUnit(new BigDecimal("100.00"))
                .wastageCategory(WastageCategory.SPOILAGE)
                .notes("Post-transit pressure bruises")
                .recordedByUserId(5L)
                .build();

        ProductWastageDto result = stockLedgerService.recordWastage(req);

        assertThat(result.getWastageReference()).startsWith("WST-");
        assertThat(result.getTotalLossAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(result.getWastageCategory()).isEqualTo(WastageCategory.SPOILAGE);

        verify(wastageRepository).save(any(FreshProductWastage.class));
        verify(movementRepository).save(argThat(m ->
                m.getMovementType() == StockMovementType.WASTAGE_WRITE_OFF
                        && m.getBalanceAfter().compareTo(new BigDecimal("45.000")) == 0
        ));
    }
}
