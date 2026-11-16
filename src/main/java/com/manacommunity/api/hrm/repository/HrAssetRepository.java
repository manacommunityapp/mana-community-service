package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HrAssetRepository extends JpaRepository<HrAsset, Long> {
    Page<HrAsset> findByOrganizationId(Long orgId, Pageable pageable);
    Page<HrAsset> findByOrganizationIdAndStatus(Long orgId, String status, Pageable pageable);
}
