package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.entity.HrEmployee;
import com.manacommunity.api.hrm.entity.HrOffboardingTask;
import com.manacommunity.api.hrm.repository.HrOffboardingTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HrOffboardingService {

    private final HrOffboardingTaskRepository taskRepository;

    private static final List<Map<String, String>> DEFAULT_TASKS = List.of(
        Map.of("title", "Complete knowledge transfer documentation", "category", "KNOWLEDGE_TRANSFER"),
        Map.of("title", "Return company equipment and assets", "category", "ASSET_RETURN"),
        Map.of("title", "Revoke system access and credentials", "category", "ACCESS_REVOCATION"),
        Map.of("title", "Conduct exit interview", "category", "EXIT_INTERVIEW"),
        Map.of("title", "Process final settlement", "category", "SETTLEMENT"),
        Map.of("title", "Issue experience and relieving letter", "category", "DOCUMENTATION"),
        Map.of("title", "Complete PF and gratuity processing", "category", "SETTLEMENT")
    );

    @Transactional
    public void initiateOffboarding(HrEmployee employee) {
        LocalDate exitDate = employee.getExitDate() != null ? employee.getExitDate() : LocalDate.now().plusDays(30);
        for (Map<String, String> task : DEFAULT_TASKS) {
            taskRepository.save(HrOffboardingTask.builder()
                .organizationId(employee.getOrganization().getId())
                .employee(employee)
                .taskTitle(task.get("title"))
                .category(task.get("category"))
                .dueDate(exitDate)
                .build());
        }
    }

    @Transactional(readOnly = true)
    public List<HrOffboardingTask> getEmployeeTasks(Long employeeId) {
        return taskRepository.findByEmployeeIdOrderByDueDateAsc(employeeId);
    }

    @Transactional
    public HrOffboardingTask completeTask(Long taskId, Long completedBy) {
        HrOffboardingTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Offboarding task not found"));
        task.setIsCompleted(true);
        task.setCompletedAt(LocalDateTime.now());
        task.setCompletedBy(completedBy);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<HrOffboardingTask> getPendingTasks(Long orgId) {
        return taskRepository.findByOrganizationIdAndIsCompletedFalse(orgId);
    }
}
