package com.manacommunity.api.clubhouse.repository;

import com.manacommunity.api.clubhouse.entity.PosTabChallengeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PosTabChallengeRepository extends JpaRepository<PosTabChallengeEntity, UUID> {
    List<PosTabChallengeEntity> findBySocietyIdAndFlatIdAndStatusOrderByCreatedAtDesc(
            Long societyId, Long flatId, String status);
}
