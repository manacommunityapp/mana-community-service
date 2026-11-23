package com.manacommunity.api.homeservices.repository;

import com.manacommunity.api.homeservices.model.StaffTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffTaskRepository extends JpaRepository<StaffTask, Long> {

    List<StaffTask> findByAssignedByAndCommunityIdOrderByCreatedAtDesc(Long assignedBy, Long communityId);

    List<StaffTask> findByStaffIdAndCommunityIdOrderByDueDateAsc(Long staffId, Long communityId);
}
