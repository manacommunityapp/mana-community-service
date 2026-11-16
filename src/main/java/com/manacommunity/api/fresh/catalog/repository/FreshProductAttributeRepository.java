package com.manacommunity.api.fresh.catalog.repository;

import com.manacommunity.api.fresh.catalog.model.FreshProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FreshProductAttributeRepository extends JpaRepository<FreshProductAttribute, Long> {
    List<FreshProductAttribute> findByVendorProductIdOrderByDisplayOrderAsc(Long vendorProductId);
    void deleteByVendorProductIdAndAttributeKey(Long vendorProductId, String attributeKey);
}