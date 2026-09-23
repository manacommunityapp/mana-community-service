package com.manacommunity.api.homeservice.repository;

import com.manacommunity.api.homeservice.model.entity.HomeServiceReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository("homeServiceReportRepository")
public interface HomeServiceReportRepository extends JpaRepository<HomeServiceReportEntity, String> {
    List<HomeServiceReportEntity> findByCommunityIdOrderByCreatedAtDesc(String communityId);
    List<HomeServiceReportEntity> findByWorkerIdOrderByCreatedAtDesc(String workerId);
}
