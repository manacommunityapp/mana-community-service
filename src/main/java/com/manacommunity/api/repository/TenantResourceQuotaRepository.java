package com.manacommunity.api.repository;

import com.manacommunity.api.model.TenantResourceQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantResourceQuotaRepository extends JpaRepository<TenantResourceQuota, Long> {

    Optional<TenantResourceQuota> findByCommunityIdAndResourceType(Long communityId, String resourceType);

    List<TenantResourceQuota> findByCommunityIdAndActiveTrue(Long communityId);
}
