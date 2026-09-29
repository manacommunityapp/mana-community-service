package com.manacommunity.api.homeservice.repository;

import com.manacommunity.api.homeservice.model.entity.HomeServiceRequirementEntity;
import com.manacommunity.api.homeservice.model.enums.HomeServiceRequirementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository("homeServiceRequirementRepository")
public interface HomeServiceRequirementRepository extends JpaRepository<HomeServiceRequirementEntity, String> {
    List<HomeServiceRequirementEntity> findByCommunityIdOrderByCreatedAtDesc(String communityId);
    List<HomeServiceRequirementEntity> findByCommunityIdAndStatusOrderByCreatedAtDesc(String communityId, HomeServiceRequirementStatus status);
    List<HomeServiceRequirementEntity> findByResidentUserIdOrderByCreatedAtDesc(String residentUserId);
}
