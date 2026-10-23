package com.manacommunity.api.homeservice.repository;

import com.manacommunity.api.homeservice.model.entity.WorkerFlatAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository("homeServiceWorkerFlatAssignmentRepository")
public interface WorkerFlatAssignmentRepository extends JpaRepository<WorkerFlatAssignmentEntity, String> {
    List<WorkerFlatAssignmentEntity> findByWorkerIdAndActiveTrue(String workerId);
    List<WorkerFlatAssignmentEntity> findByCommunityIdAndTower(String communityId, String tower);
    List<WorkerFlatAssignmentEntity> findByResidentUserIdAndActiveTrue(String residentUserId);
}
