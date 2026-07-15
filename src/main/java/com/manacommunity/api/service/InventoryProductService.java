package com.manacommunity.api.service;

import com.manacommunity.api.dto.InventoryProductRequest;
import com.manacommunity.api.dto.InventoryProductResponse;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.InventoryCategory;
import com.manacommunity.api.model.InventoryProduct;
import com.manacommunity.api.repository.InventoryCategoryRepository;
import com.manacommunity.api.repository.InventoryProductRepository;
import com.manacommunity.api.repository.InventoryStockQuantRepository;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryProductService {

    private final InventoryProductRepository productRepo;
    private final InventoryCategoryRepository categoryRepo;
    private final InventoryStockQuantRepository quantRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<InventoryProductResponse> getAll() {
        return productRepo.findByIsActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryProductResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<InventoryProductResponse> search(String query) {
        return productRepo.search(query).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InventoryProductResponse create(InventoryProductRequest req) {
        InventoryProduct product = InventoryProduct.builder()
                .name(req.name())
                .defaultCode(req.defaultCode())
                .barcode(req.barcode())
                .listPrice(req.listPrice() != null ? req.listPrice() : BigDecimal.ZERO)
                .standardPrice(req.standardPrice() != null ? req.standardPrice() : BigDecimal.ZERO)
                .type(req.type() != null ? InventoryProduct.ProductType.valueOf(req.type()) : InventoryProduct.ProductType.STORABLE)
                .tracking(req.tracking() != null ? InventoryProduct.TrackingType.valueOf(req.tracking()) : InventoryProduct.TrackingType.NONE)
                .build();

        if (req.categId() != null) {
            InventoryCategory cat = categoryRepo.findById(req.categId())
                    .orElseThrow(() -> new ResourceNotFoundException("InventoryCategory", req.categId()));
            product.setCategory(cat);
        }

        InventoryProduct saved = productRepo.save(product);
        log.info("Created inventory product: id={}, name={}", saved.getId(), saved.getName());

        auditService.record(AuditAction.PRODUCT_CREATED, AuditModule.ADMIN,
                "InventoryProduct", String.valueOf(saved.getId()), null,
                "name=" + saved.getName() + ", sku=" + saved.getDefaultCode() + ", type=" + saved.getType());

        return toResponse(saved);
    }

    @Transactional
    public InventoryProductResponse update(Long id, InventoryProductRequest req) {
        InventoryProduct product = findOrThrow(id);

        String oldValue = "name=" + product.getName() + ", listPrice=" + product.getListPrice()
                + ", standardPrice=" + product.getStandardPrice();

        product.setName(req.name());
        product.setDefaultCode(req.defaultCode());
        product.setBarcode(req.barcode());
        if (req.listPrice() != null) product.setListPrice(req.listPrice());
        if (req.standardPrice() != null) product.setStandardPrice(req.standardPrice());
        if (req.type() != null) product.setType(InventoryProduct.ProductType.valueOf(req.type()));
        if (req.tracking() != null) product.setTracking(InventoryProduct.TrackingType.valueOf(req.tracking()));

        if (req.categId() != null) {
            InventoryCategory cat = categoryRepo.findById(req.categId())
                    .orElseThrow(() -> new ResourceNotFoundException("InventoryCategory", req.categId()));
            product.setCategory(cat);
        }

        InventoryProduct saved = productRepo.save(product);
        String newValue = "name=" + saved.getName() + ", listPrice=" + saved.getListPrice()
                + ", standardPrice=" + saved.getStandardPrice();

        auditService.record(AuditAction.PRODUCT_UPDATED, AuditModule.ADMIN,
                "InventoryProduct", String.valueOf(id), oldValue, newValue);

        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        InventoryProduct product = findOrThrow(id);
        String snapshot = "name=" + product.getName() + ", sku=" + product.getDefaultCode();
        product.setIsActive(false);
        productRepo.save(product);

        auditService.record(AuditAction.PRODUCT_DELETED, AuditModule.ADMIN,
                "InventoryProduct", String.valueOf(id), snapshot, null);
    }

    private InventoryProduct findOrThrow(Long id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryProduct", id));
    }

    private InventoryProductResponse toResponse(InventoryProduct p) {
        BigDecimal qty = quantRepo.sumOnHandByProduct(p.getId());
        return new InventoryProductResponse(
                p.getId(), p.getName(), p.getDefaultCode(), p.getBarcode(),
                p.getListPrice(), p.getStandardPrice(),
                p.getType().name(), p.getTracking().name(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                qty
        );
    }
}
