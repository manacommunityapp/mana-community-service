package com.manacommunity.api.repository;

import com.manacommunity.api.model.WaterTreatmentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WaterTreatmentLogRepository extends JpaRepository<WaterTreatmentLog, Long> {

    List<WaterTreatmentLog> findByCommunityIdOrderByLogDateDesc(Long communityId);

    List<WaterTreatmentLog> findByCommunityIdAndPlantTypeOrderByLogDateDesc(Long communityId, String plantType);
}
