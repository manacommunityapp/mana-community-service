package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.entity.HrEmployee;
import com.manacommunity.api.hrm.entity.HrOnboardingTask;
import com.manacommunity.api.hrm.repository.HrOnboardingTaskRepository;
import com.manacommunity.api.hrm.repository.HrOnboardingTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HrOnboardingService {

    private final HrOnboardingTemplateRepository templateRepository;
    private final HrOnboardingTaskRepository taskRepository;

    private static final List<Map<String, String>> DEFAULT_TASKS = List.of(
        Map.of("title", "Collect identity documents (Aadhaar, PAN)", "category", "DOCUMENTATION"),
        Map.of("title", "Complete employment agreement signing", "category", "DOCUMENTATION"),
        Map.of("title", "Set up work email and system access", "category", "IT_SETUP"),
        Map.of("title", "Assign uniform and equipment", "category", "EQUIPMENT"),
        Map.of("title", "Complete HR induction", "category", "INDUCTION"),
        Map.of("title", "Introduction to team and workspace", "category", "INDUCTION"),
        Map.of("title", "Configure payroll bank details", "category", "DOCUMENTATION")
    );

    @Transactional
    public void initiateOnboarding(HrEmployee employee) {
        LocalDate joiningDate = employee.getJoiningDate() != null ? employee.getJoiningDate() : LocalDate.now();
        for (Map<String, String> task : DEFAULT_TASKS) {
            taskRepository.save(HrOnboardingTask.builder()
                .organizationId(employee.getOrganization().getId())
                .employee(employee)
                .taskTitle(task.get("title"))
                .category(task.get("category"))
                .dueDate(joiningDate.plusDays(7))
                .build());
        }
    }

    @Transactional(readOnly = true)
    public List<HrOnboardingTask> getEmployeeTasks(Long employeeId) {
        return taskRepository.findByEmployeeIdOrderByDueDateAsc(employeeId);
    }

    @Transactional
    public HrOnboardingTask completeTask(Long taskId, Long completedBy) {
        HrOnboardingTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Onboarding task not found"));
        task.setIsCompleted(true);
        task.setCompletedAt(LocalDateTime.now());
        task.setCompletedBy(completedBy);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<HrOnboardingTask> getPendingTasks(Long orgId) {
        return taskRepository.findByOrganizationIdAndIsCompletedFalse(orgId);
    }
}
