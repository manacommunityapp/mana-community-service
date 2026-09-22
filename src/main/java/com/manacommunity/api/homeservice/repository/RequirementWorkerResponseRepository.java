package com.manacommunity.api.homeservice.repository;

import com.manacommunity.api.homeservice.model.entity.RequirementWorkerResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository("homeServiceRequirementWorkerResponseRepository")
public interface RequirementWorkerResponseRepository extends JpaRepository<RequirementWorkerResponseEntity, String> {
    List<RequirementWorkerResponseEntity> findByRequestIdOrderByCreatedAtAsc(String requestId);
    List<RequirementWorkerResponseEntity> findByWorkerIdOrderByCreatedAtDesc(String workerId);
}
