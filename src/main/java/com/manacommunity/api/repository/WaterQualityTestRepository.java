package com.manacommunity.api.repository;

import com.manacommunity.api.model.WaterQualityTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WaterQualityTestRepository extends JpaRepository<WaterQualityTest, Long> {

    List<WaterQualityTest> findByCommunityIdOrderByTestDateDesc(Long communityId);

    List<WaterQualityTest> findByCommunityIdAndSourceLocationOrderByTestDateDesc(Long communityId, String sourceLocation);
}
