package com.manacommunity.api.service;

import com.manacommunity.api.dto.InventoryLotRequest;
import com.manacommunity.api.dto.InventoryLotResponse;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.InventoryLot;
import com.manacommunity.api.model.InventoryProduct;
import com.manacommunity.api.repository.InventoryLotRepository;
import com.manacommunity.api.repository.InventoryProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryLotService {

    private final InventoryLotRepository lotRepo;
    private final InventoryProductRepository productRepo;

    @Transactional(readOnly = true)
    public List<InventoryLotResponse> getByProduct(Long productId) {
        return lotRepo.findByProductIdOrderByNameAsc(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryLotResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public InventoryLotResponse create(InventoryLotRequest req) {
        InventoryProduct product = productRepo.findById(req.productId())
                .orElseThrow(() -> new ResourceNotFoundException("InventoryProduct", req.productId()));

        // Validate tracking is enabled
        if (product.getTracking() == InventoryProduct.TrackingType.NONE) {
            throw new IllegalArgumentException(
                    "Product '" + product.getName() + "' has tracking=NONE. Enable LOT or SERIAL tracking before creating lots.");
        }

        // Enforce uniqueness
        if (lotRepo.existsByNameAndProductId(req.name(), req.productId())) {
            throw new IllegalArgumentException(
                    "Lot '" + req.name() + "' already exists for product '" + product.getName() + "'.");
        }

        InventoryLot lot = InventoryLot.builder()
                .name(req.name())
                .product(product)
                .expirationDate(req.expirationDate())
                .notes(req.notes())
                .build();

        InventoryLot saved = lotRepo.save(lot);
        log.info("Created lot: id={}, name={}, productId={}", saved.getId(), saved.getName(), product.getId());
        return toResponse(saved);
    }

    @Transactional
    public InventoryLotResponse getOrCreate(String name, Long productId) {
        return lotRepo.findByNameAndProductId(name, productId)
                .map(this::toResponse)
                .orElseGet(() -> create(new InventoryLotRequest(name, productId, null, null)));
    }

    private InventoryLot findOrThrow(Long id) {
        return lotRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryLot", id));
    }

    private InventoryLotResponse toResponse(InventoryLot lot) {
        return new InventoryLotResponse(
                lot.getId(), lot.getName(),
                lot.getProduct().getId(), lot.getProduct().getName(),
                lot.getExpirationDate(), lot.getNotes(), lot.getCreatedAt()
        );
    }
}
