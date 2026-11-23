package com.manacommunity.api.homeservices.service;

import com.manacommunity.api.homeservices.model.StaffTask;
import com.manacommunity.api.homeservices.repository.StaffTaskRepository;
import com.manacommunity.api.model.Community;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffTaskService {

    private final StaffTaskRepository taskRepo;
    private final EntityManager em;

    public StaffTask createTask(Long communityId, Long staffId, Long assignedBy,
                                 String title, String description, LocalDate dueDate) {
        Community community = em.getReference(Community.class, communityId);
        StaffTask task = StaffTask.builder()
                .community(community)
                .staffId(staffId)
                .assignedBy(assignedBy)
                .title(title)
                .description(description)
                .dueDate(dueDate)
                .build();
        return taskRepo.save(task);
    }

    public StaffTask updateStatus(Long taskId, Long communityId, StaffTask.TaskStatus newStatus) {
        StaffTask task = taskRepo.findById(taskId)
                .filter(t -> t.getCommunity().getId().equals(communityId))
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        task.setStatus(newStatus);
        if (newStatus == StaffTask.TaskStatus.COMPLETED) {
            task.setCompletedAt(Instant.now());
        }
        return taskRepo.save(task);
    }

    @Transactional(readOnly = true)
    public List<StaffTask> getTasksForResident(Long residentUserId, Long communityId) {
        return taskRepo.findByAssignedByAndCommunityIdOrderByCreatedAtDesc(residentUserId, communityId);
    }

    @Transactional(readOnly = true)
    public List<StaffTask> getTasksForStaff(Long staffId, Long communityId) {
        return taskRepo.findByStaffIdAndCommunityIdOrderByDueDateAsc(staffId, communityId);
    }
}
