package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.ProductStockStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProductStockStatusRepository extends JpaRepository<ProductStockStatus, Long> {
    Optional<ProductStockStatus> findByVendorIdAndProductIdAndProductType(Long vendorId, Long productId, String productType);
}
