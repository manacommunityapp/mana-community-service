package com.manacommunity.api.finops.repository;

import com.manacommunity.api.finops.entity.StorageTierPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageTierPolicyRepository extends JpaRepository<StorageTierPolicyEntity, String> {
}
