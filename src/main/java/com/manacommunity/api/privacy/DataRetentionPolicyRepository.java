package com.manacommunity.api.privacy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataRetentionPolicyRepository extends JpaRepository<DataRetentionPolicy, Long> {

    List<DataRetentionPolicy> findByCommunityIdOrCommunityIdIsNull(Long communityId);

    Optional<DataRetentionPolicy> findByDataCategoryAndCommunityId(String dataCategory, Long communityId);

    Optional<DataRetentionPolicy> findByDataCategoryAndCommunityIdIsNull(String dataCategory);

    List<DataRetentionPolicy> findByIsActiveTrue();
}
