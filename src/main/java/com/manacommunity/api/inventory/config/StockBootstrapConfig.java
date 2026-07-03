package com.manacommunity.api.inventory.config;

import com.manacommunity.api.model.Community;

import com.manacommunity.api.inventory.entity.*;
import com.manacommunity.api.inventory.repository.*;
import com.manacommunity.api.inventory.service.StockQuantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class StockBootstrapConfig {

    private final ProductRepository productRepository;
    private final StockWarehouseRepository warehouseRepository;
    private final StockLocationRepository locationRepository;
    private final StockPickingTypeRepository pickingTypeRepository;
    private final StockQuantService quantService;
    private final StockPickingRepository pickingRepository;
    private final StockMoveRepository moveRepository;

    @Bean
    @Order(10)
    public CommandLineRunner bootstrapStockData() {
        return args -> {
            try {
                log.info("Checking and bootstrapping double-entry inventory data...");

                // 1. Bootstrap Products Catalog
                if (productRepository.count() == 0) {
                    log.info("Bootstrapping product catalog...");
                    productRepository.save(Product.builder().name("Lawn Mower").sku("LMW-001").barcode("111111").description("Community physical lawn mower").price(new BigDecimal("299.99")).build());
                    productRepository.save(Product.builder().name("Power Drill").sku("PDR-002").barcode("222222").description("High-torque electric drill").price(new BigDecimal("89.99")).build());
                    productRepository.save(Product.builder().name("Extension Ladder").sku("EXL-003").barcode("333333").description("24ft aluminum ladder").price(new BigDecimal("149.99")).build());
                    productRepository.save(Product.builder().name("PS5 Console").sku("PS5-004").barcode("444444").description("Gaming entertainment unit").price(new BigDecimal("499.99")).build());
                    productRepository.save(Product.builder().name("Projector").sku("PRJ-005").barcode("555555").description("1080p home theater projector").price(new BigDecimal("349.99")).build());
                }

                // 2. Bootstrap Locations & Warehouses
                if (warehouseRepository.count() == 0) {
                    log.info("Bootstrapping warehouses and stock locations...");

                    // Virtual Root & Sub-locations
                    StockLocation virtualView = getOrCreateLocation("Virtual", LocationUsage.view, null, null);
                    StockLocation vendors = getOrCreateLocation("Virtual/Vendors", LocationUsage.vendor, virtualView.getId(), null);
                    StockLocation customers = getOrCreateLocation("Virtual/Customers", LocationUsage.customer, virtualView.getId(), null);
                    StockLocation inventoryAdj = getOrCreateLocation("Virtual/Inventory Adjustment", LocationUsage.inventory, virtualView.getId(), null);
                    StockLocation scrap = getOrCreateLocation("Virtual/Scrap", LocationUsage.scrap, virtualView.getId(), null);

                    // Warehouse Header
                    StockWarehouse wh = StockWarehouse.builder()
                            .name("Main Warehouse")
                            .code("WH")
                            .receptionSteps("1_step")
                            .deliverySteps("1_step")
                            .build();
                    wh = warehouseRepository.save(wh);

                    // Physical Locations inside Warehouse
                    StockLocation whView = getOrCreateLocation("WH", LocationUsage.view, null, wh.getId());
                    StockLocation whStock = getOrCreateLocation("WH/Stock", LocationUsage.internal, whView.getId(), wh.getId());
                    StockLocation whOutput = getOrCreateLocation("WH/Output", LocationUsage.internal, whView.getId(), wh.getId());

                    // Update Warehouse default stock location link
                    wh.setLotStockId(whStock.getId());
                    warehouseRepository.save(wh);

                    // 3. Bootstrap Picking Types (Operation Types Templates)
                    log.info("Bootstrapping stock picking types...");
                    StockPickingType incoming = StockPickingType.builder()
                            .name("Receipts")
                            .code(PickingTypeCode.incoming)
                            .warehouseId(wh.getId())
                            .defaultLocationSrcId(vendors.getId())
                            .defaultLocationDestId(whStock.getId())
                            .build();
                    incoming = pickingTypeRepository.save(incoming);

                    StockPickingType outgoing = StockPickingType.builder()
                            .name("Deliveries")
                            .code(PickingTypeCode.outgoing)
                            .warehouseId(wh.getId())
                            .defaultLocationSrcId(whStock.getId())
                            .defaultLocationDestId(customers.getId())
                            .build();
                    outgoing = pickingTypeRepository.save(outgoing);

                    StockPickingType internal = StockPickingType.builder()
                            .name("Internal Transfers")
                            .code(PickingTypeCode.internal)
                            .warehouseId(wh.getId())
                            .defaultLocationSrcId(whStock.getId())
                            .defaultLocationDestId(whOutput.getId())
                            .build();
                    pickingTypeRepository.save(internal);

                    // 4. Populate Initial Stock Levels in WH/Stock using quantService
                    log.info("Populating initial stock balances inside WH/Stock...");
                    productRepository.findBySku("LMW-001").ifPresent(p -> quantService.adjustStock(p.getId(), whStock.getId(), null, 50.0));
                    productRepository.findBySku("PDR-002").ifPresent(p -> quantService.adjustStock(p.getId(), whStock.getId(), null, 100.0));
                    productRepository.findBySku("EXL-003").ifPresent(p -> quantService.adjustStock(p.getId(), whStock.getId(), null, 40.0));
                    productRepository.findBySku("PS5-004").ifPresent(p -> quantService.adjustStock(p.getId(), whStock.getId(), null, 30.0));
                    productRepository.findBySku("PRJ-005").ifPresent(p -> quantService.adjustStock(p.getId(), whStock.getId(), null, 60.0));

                    // 5. Seed Pickings to match Osius Design Reference:
                    // Receipts: 8 To Process, 2 Late, 1 Backorders
                    // Delivery Orders: 14 To Process, 3 Late, 5 Backorders
                    log.info("Bootstrapping mock transfers for Receipts and Delivery Orders...");
                    List<Product> products = productRepository.findAll();
                    Product defaultProd = products.isEmpty() ? null : products.get(0);

                    // A. Receipts (8 total)
                    for (int i = 0; i < 8; i++) {
                        boolean isLate = (i < 2);
                        boolean isBackorder = (i == 7);
                        
                        LocalDateTime schedDate = isLate ? LocalDateTime.now().minusDays(2) : LocalDateTime.now().plusDays(2);
                        String origin = isBackorder ? "PO00244 - backorder" : "PO00" + (238 + i);
                        
                        StockPicking picking = StockPicking.builder()
                                .name("WH/IN/00" + (238 + i))
                                .origin(origin)
                                .state(PickingState.ready)
                                .scheduledDate(schedDate)
                                .locationId(vendors.getId())
                                .locationDestId(whStock.getId())
                                .pickingTypeId(incoming.getId())
                                .build();
                        picking = pickingRepository.save(picking);

                        if (defaultProd != null) {
                            moveRepository.save(StockMove.builder()
                                    .productId(defaultProd.getId())
                                    .productUomQty(10.0 * (i + 1))
                                    .quantity(0.0)
                                    .state(MoveState.assigned)
                                    .pickingId(picking.getId())
                                    .locationId(vendors.getId())
                                    .build());
                        }
                    }

                    // B. Deliveries (14 total)
                    for (int i = 0; i < 14; i++) {
                        boolean isLate = (i < 3);
                        boolean isBackorder = (i >= 9 && i <= 13); // 5 backorders
                        
                        LocalDateTime schedDate = isLate ? LocalDateTime.now().minusDays(3) : LocalDateTime.now().plusDays(3);
                        String origin = isBackorder ? "SO0024" + i + " - backorder" : "SO00" + (238 + i);
                        
                        StockPicking picking = StockPicking.builder()
                                .name("WH/OUT/00" + (238 + i))
                                .origin(origin)
                                .state(PickingState.ready)
                                .scheduledDate(schedDate)
                                .locationId(whStock.getId())
                                .locationDestId(customers.getId())
                                .pickingTypeId(outgoing.getId())
                                .build();
                        picking = pickingRepository.save(picking);

                        if (defaultProd != null) {
                            moveRepository.save(StockMove.builder()
                                    .productId(defaultProd.getId())
                                    .productUomQty(5.0 * (i + 1))
                                    .quantity(0.0)
                                    .state(MoveState.assigned)
                                    .pickingId(picking.getId())
                                    .locationId(whStock.getId())
                                    .build());
                        }
                    }
                }

                log.info("Double-entry stock inventory system successfully bootstrapped!");
            } catch (Exception e) {
                log.error("Failed to bootstrap double-entry inventory data", e);
            }
        };
    }

    private StockLocation getOrCreateLocation(String completeName, LocationUsage usage, Long parentId, Long warehouseId) {
        Optional<StockLocation> opt = locationRepository.findByCompleteName(completeName);
        if (opt.isPresent()) {
            return opt.get();
        }
        StockLocation loc = StockLocation.builder()
                .completeName(completeName)
                .usage(usage)
                .locationId(parentId)
                .warehouseId(warehouseId)
                .build();
        return locationRepository.save(loc);
    }
}
