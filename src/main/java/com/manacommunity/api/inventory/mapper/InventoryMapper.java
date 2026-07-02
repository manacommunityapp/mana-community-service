package com.manacommunity.api.inventory.mapper;

import com.manacommunity.api.inventory.dto.*;
import com.manacommunity.api.inventory.entity.*;

public class InventoryMapper {

    public static ProductDto toDto(Product p) {
        if (p == null) return null;
        return ProductDto.builder()
                .id(p.getId())
                .name(p.getName())
                .sku(p.getSku())
                .barcode(p.getBarcode())
                .description(p.getDescription())
                .price(p.getPrice())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    public static Product toEntity(ProductDto dto) {
        if (dto == null) return null;
        return Product.builder()
                .id(dto.getId())
                .name(dto.getName())
                .sku(dto.getSku())
                .barcode(dto.getBarcode())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .build();
    }

    public static WarehouseDto toDto(StockWarehouse w) {
        if (w == null) return null;
        return WarehouseDto.builder()
                .id(w.getId())
                .name(w.getName())
                .code(w.getCode())
                .partnerId(w.getPartnerId())
                .lotStockId(w.getLotStockId())
                .receptionSteps(w.getReceptionSteps())
                .deliverySteps(w.getDeliverySteps())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }

    public static LocationDto toDto(StockLocation l) {
        if (l == null) return null;
        return LocationDto.builder()
                .id(l.getId())
                .completeName(l.getCompleteName())
                .usage(l.getUsage())
                .locationId(l.getLocationId())
                .barcode(l.getBarcode())
                .warehouseId(l.getWarehouseId())
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }

    public static QuantDto toDto(StockQuant q, String productName, String productSku, String locationName, String lotName) {
        if (q == null) return null;
        return QuantDto.builder()
                .id(q.getId())
                .productId(q.getProductId())
                .productName(productName)
                .productSku(productSku)
                .locationId(q.getLocationId())
                .locationCompleteName(locationName)
                .quantity(q.getQuantity())
                .reservedQuantity(q.getReservedQuantity())
                .lotId(q.getLotId())
                .lotName(lotName)
                .inDate(q.getInDate())
                .updatedAt(q.getUpdatedAt())
                .build();
    }

    public static LotDto toDto(StockLot l, String productName) {
        if (l == null) return null;
        return LotDto.builder()
                .id(l.getId())
                .name(l.getName())
                .productId(l.getProductId())
                .productName(productName)
                .productQty(l.getProductQty())
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }

    public static PickingDto toDto(StockPicking p, String srcLocName, String destLocName, String typeName) {
        if (p == null) return null;
        return PickingDto.builder()
                .id(p.getId())
                .name(p.getName())
                .origin(p.getOrigin())
                .state(p.getState())
                .scheduledDate(p.getScheduledDate())
                .locationId(p.getLocationId())
                .locationName(srcLocName)
                .locationDestId(p.getLocationDestId())
                .locationDestName(destLocName)
                .pickingTypeId(p.getPickingTypeId())
                .pickingTypeName(typeName)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    public static MoveDto toDto(StockMove m, String productName, String locationName) {
        if (m == null) return null;
        return MoveDto.builder()
                .id(m.getId())
                .productId(m.getProductId())
                .productName(productName)
                .productUomQty(m.getProductUomQty())
                .quantity(m.getQuantity())
                .state(m.getState())
                .pickingId(m.getPickingId())
                .locationId(m.getLocationId())
                .locationName(locationName)
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    public static MoveLineDto toDto(StockMoveLine ml, String productName, String lotName, String srcLocName, String destLocName) {
        if (ml == null) return null;
        return MoveLineDto.builder()
                .id(ml.getId())
                .productId(ml.getProductId())
                .productName(productName)
                .lotId(ml.getLotId())
                .lotName(lotName)
                .quantity(ml.getQuantity())
                .locationId(ml.getLocationId())
                .locationName(srcLocName)
                .locationDestId(ml.getLocationDestId())
                .locationDestName(destLocName)
                .resultPackageId(ml.getResultPackageId())
                .moveId(ml.getMoveId())
                .pickingId(ml.getPickingId())
                .createdAt(ml.getCreatedAt())
                .updatedAt(ml.getUpdatedAt())
                .build();
    }
}
