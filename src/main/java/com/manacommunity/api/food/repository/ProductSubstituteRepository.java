package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.ProductSubstitute;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductSubstituteRepository extends JpaRepository<ProductSubstitute, Long> {
    List<ProductSubstitute> findByVendorIdAndOriginalProductIdAndOriginalProductTypeAndIsActiveTrueOrderByPriorityAsc(Long vendorId, Long productId, String productType);
}
