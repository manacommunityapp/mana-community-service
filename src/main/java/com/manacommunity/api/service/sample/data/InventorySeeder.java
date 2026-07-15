package com.manacommunity.api.service.sample.data;

import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventorySeeder {

    private final InventoryWarehouseRepository warehouseRepo;
    private final InventoryLocationRepository locationRepo;
    private final InventoryCategoryRepository categoryRepo;
    private final InventoryPickingTypeRepository pickingTypeRepo;
    private final InventorySequenceRepository sequenceRepo;

    @Transactional
    public void seed() {
        log.info("Initializing inventory module seeds...");

        // 1. Categories
        if (categoryRepo.count() == 0) {
            categoryRepo.save(InventoryCategory.builder().name("All / Saleable").build());
            categoryRepo.save(InventoryCategory.builder().name("Raw Materials").build());
            categoryRepo.save(InventoryCategory.builder().name("Finished Goods").build());
            categoryRepo.save(InventoryCategory.builder().name("Consumables").build());
            log.info("Seeded default inventory categories.");
        }

        // 2. Warehouse
        InventoryWarehouse wh = warehouseRepo.findByCode("WH").orElseGet(() -> {
            InventoryWarehouse w = InventoryWarehouse.builder()
                    .name("Main Warehouse")
                    .code("WH")
                    .receptionSteps(InventoryWarehouse.StepType.ONE_STEP)
                    .deliverySteps(InventoryWarehouse.StepType.ONE_STEP)
                    .build();
            InventoryWarehouse saved = warehouseRepo.save(w);
            log.info("Seeded default Main Warehouse.");
            return saved;
        });

        // 3. Locations
        InventoryLocation stockLoc = getOrCreateLocation("WH/Stock", InventoryLocation.LocationUsage.INTERNAL, wh);
        getOrCreateLocation("WH/Input", InventoryLocation.LocationUsage.INTERNAL, wh);
        getOrCreateLocation("WH/Output", InventoryLocation.LocationUsage.INTERNAL, wh);
        getOrCreateLocation("WH/Quality", InventoryLocation.LocationUsage.INTERNAL, wh);
        InventoryLocation vendorLoc = getOrCreateLocation("Partner/Vendor", InventoryLocation.LocationUsage.VENDOR, wh);
        InventoryLocation customerLoc = getOrCreateLocation("Partner/Customer", InventoryLocation.LocationUsage.CUSTOMER, wh);
        getOrCreateLocation("Virtual/Scrap", InventoryLocation.LocationUsage.SCRAP, wh);
        getOrCreateLocation("Virtual/Inventory Adjustment", InventoryLocation.LocationUsage.INVENTORY, wh);

        // 4. Sequences
        getOrCreateSequence("WH/IN/");
        getOrCreateSequence("WH/OUT/");
        getOrCreateSequence("WH/INT/");

        // 5. Picking Types
        getOrCreatePickingType("Receipts", InventoryPickingType.PickingTypeCode.INCOMING, "WH/IN/", wh, vendorLoc, stockLoc);
        getOrCreatePickingType("Deliveries", InventoryPickingType.PickingTypeCode.OUTGOING, "WH/OUT/", wh, stockLoc, customerLoc);
        getOrCreatePickingType("Internal", InventoryPickingType.PickingTypeCode.INTERNAL, "WH/INT/", wh, stockLoc, stockLoc);

        log.info("Inventory module seeding completed successfully.");
    }

    private InventoryLocation getOrCreateLocation(String completeName, InventoryLocation.LocationUsage usage, InventoryWarehouse wh) {
        return locationRepo.findByIsActiveTrueOrderByCompleteNameAsc().stream()
                .filter(l -> l.getCompleteName().equals(completeName))
                .findFirst()
                .orElseGet(() -> {
                    InventoryLocation loc = InventoryLocation.builder()
                            .completeName(completeName)
                            .usage(usage)
                            .warehouse(wh)
                            .build();
                    InventoryLocation saved = locationRepo.save(loc);
                    log.info("Seeded default location: {}", completeName);
                    return saved;
                });
    }

    private void getOrCreateSequence(String prefix) {
        sequenceRepo.findByPrefixForUpdate(prefix).orElseGet(() -> {
            InventorySequence seq = InventorySequence.builder()
                    .prefix(prefix)
                    .nextVal(1L)
                    .build();
            sequenceRepo.save(seq);
            log.info("Seeded default sequence: {}", prefix);
            return seq;
        });
    }

    private void getOrCreatePickingType(String name, InventoryPickingType.PickingTypeCode code, String prefix,
                                        InventoryWarehouse wh, InventoryLocation src, InventoryLocation dest) {
        pickingTypeRepo.findAllByOrderByCodeAsc().stream()
                .filter(pt -> pt.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> {
                    InventoryPickingType pt = InventoryPickingType.builder()
                            .name(name)
                            .code(code)
                            .sequencePrefix(prefix)
                            .warehouse(wh)
                            .defaultLocationSrc(src)
                            .defaultLocationDest(dest)
                            .build();
                    pickingTypeRepo.save(pt);
                    log.info("Seeded default picking type: {}", name);
                    return pt;
                });
    }
}
