package com.manacommunity.api.food.service;

import com.manacommunity.api.food.entity.ProductStockStatus;
import com.manacommunity.api.food.entity.ProductSubstitute;
import com.manacommunity.api.food.repository.ProductStockStatusRepository;
import com.manacommunity.api.food.repository.ProductSubstituteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductStockService {

    private final ProductStockStatusRepository stockRepo;
    private final ProductSubstituteRepository substituteRepo;

    @Transactional
    public void markOutOfStock(Long vendorId, Long productId, String productType, Long communityId, Long updatedBy, LocalDate restockDate) {
        ProductStockStatus s = stockRepo.findByVendorIdAndProductIdAndProductType(vendorId, productId, productType)
            .orElse(ProductStockStatus.builder().vendorId(vendorId).productId(productId).productType(productType).communityId(communityId).build());
        s.setInStock(false);
        s.setRestockDate(restockDate);
        s.setUpdatedBy(updatedBy);
        s.setUpdatedAt(Instant.now());
        stockRepo.save(s);
    }

    @Transactional
    public void markInStock(Long vendorId, Long productId, String productType, Long updatedBy) {
        stockRepo.findByVendorIdAndProductIdAndProductType(vendorId, productId, productType).ifPresent(s -> {
            s.setInStock(true);
            s.setRestockDate(null);
            s.setUpdatedBy(updatedBy);
            s.setUpdatedAt(Instant.now());
            stockRepo.save(s);
        });
    }

    public boolean checkStock(Long vendorId, Long productId, String productType) {
        return stockRepo.findByVendorIdAndProductIdAndProductType(vendorId, productId, productType)
            .map(ProductStockStatus::isInStock).orElse(true);
    }

    public List<ProductSubstitute> getSubstitutes(Long vendorId, Long productId, String productType) {
        return substituteRepo.findByVendorIdAndOriginalProductIdAndOriginalProductTypeAndIsActiveTrueOrderByPriorityAsc(vendorId, productId, productType);
    }

    @Transactional
    public ProductSubstitute addSubstitute(ProductSubstitute sub) {
        sub.setActive(true);
        sub.setCreatedAt(Instant.now());
        return substituteRepo.save(sub);
    }

    @Transactional
    public void removeSubstitute(Long id) {
        substituteRepo.findById(id).ifPresent(s -> { s.setActive(false); substituteRepo.save(s); });
    }
}
