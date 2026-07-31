package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsVendorCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VmsVendorCategoryRepository extends JpaRepository<VmsVendorCategory, Long> {
    List<VmsVendorCategory> findByCommunityIdAndActiveTrue(Long communityId);
    List<VmsVendorCategory> findByCommunityIdAndParentIdIsNullAndActiveTrue(Long communityId);
    List<VmsVendorCategory> findByParentIdAndActiveTrue(Long parentId);
}
