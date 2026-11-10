package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrPerformanceService {
    private final HrAppraisalCycleRepository cycleRepository;
    private final HrAppraisalFormRepository formRepository;
    private final HrGoalRepository goalRepository;
    private final HrOkrRepository okrRepository;
    private final HrOkrKeyResultRepository keyResultRepository;
    private final HrFeedbackRequestRepository feedbackRequestRepository;
    private final HrFeedbackResponseRepository feedbackResponseRepository;
    private final HrEmployeeRepository employeeRepository;

    @Transactional
    public HrAppraisalCycle createCycle(Long orgId, HrAppraisalCycle cycle) {
        cycle.setOrganizationId(orgId);
        return cycleRepository.save(cycle);
    }

    @Transactional
    public HrAppraisalCycle activateCycle(Long orgId, Long cycleId) {
        HrAppraisalCycle cycle = cycleRepository.findById(cycleId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Cycle not found"));
        if (!cycle.getOrganizationId().equals(orgId))
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        cycle.setStatus("ACTIVE");
        return cycleRepository.save(cycle);
    }

    public List<HrAppraisalCycle> getCycles(Long orgId) {
        return cycleRepository.findByOrganizationId(orgId);
    }

    @Transactional
    public HrGoal createGoal(Long orgId, Long employeeId, HrGoal goal) {
        HrEmployee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Employee not found"));
        goal.setOrganizationId(orgId);
        goal.setEmployee(employee);
        return goalRepository.save(goal);
    }

    @Transactional
    public HrGoal updateGoalProgress(Long goalId, Integer achievementPercent) {
        HrGoal goal = goalRepository.findById(goalId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Goal not found"));
        goal.setAchievementPercent(achievementPercent);
        if (achievementPercent >= 100) goal.setStatus("COMPLETED");
        return goalRepository.save(goal);
    }

    public List<HrGoal> getEmployeeGoals(Long employeeId, Long cycleId) {
        if (cycleId != null) return goalRepository.findByEmployeeIdAndAppraisalCycleId(employeeId, cycleId);
        return goalRepository.findByEmployeeId(employeeId);
    }

    @Transactional
    public HrAppraisalForm submitSelfAppraisal(Long orgId, Long employeeId, Long cycleId, Integer rating, String comments) {
        HrEmployee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Employee not found"));
        HrAppraisalCycle cycle = cycleRepository.findById(cycleId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Cycle not found"));

        HrAppraisalForm form = formRepository.findByEmployeeIdAndAppraisalCycleId(employeeId, cycleId)
            .orElseGet(() -> HrAppraisalForm.builder()
                .organizationId(orgId)
                .employee(employee)
                .appraisalCycle(cycle)
                .build());

        form.setSelfRating(rating);
        form.setSelfComments(comments);
        form.setStatus("PENDING_MANAGER");
        form.setSubmittedAt(LocalDateTime.now());
        return formRepository.save(form);
    }

    @Transactional
    public HrAppraisalForm submitManagerAppraisal(Long formId, Integer rating, String comments, Long reviewerId) {
        HrAppraisalForm form = formRepository.findById(formId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Form not found"));
        form.setManagerRating(rating);
        form.setManagerComments(comments);
        employeeRepository.findById(reviewerId).ifPresent(form::setReviewer);
        form.setStatus("COMPLETED");
        form.setReviewedAt(LocalDateTime.now());
        if (form.getSelfRating() != null) {
            form.setFinalRating((form.getSelfRating() + rating) / 2);
        } else {
            form.setFinalRating(rating);
        }
        return formRepository.save(form);
    }

    public List<HrAppraisalForm> getCycleForms(Long cycleId) {
        return formRepository.findByAppraisalCycleId(cycleId);
    }

    public List<HrAppraisalForm> getPendingReviews(Long reviewerEmployeeId) {
        return formRepository.findByReviewerEmployeeIdAndStatus(reviewerEmployeeId, "PENDING_MANAGER");
    }

    @Transactional
    public HrOkr createOkr(Long orgId, Long employeeId, HrOkr okr, List<HrOkrKeyResult> keyResults) {
        okr.setOrganizationId(orgId);
        if (employeeId != null) {
            employeeRepository.findById(employeeId).ifPresent(okr::setEmployee);
        }
        HrOkr saved = okrRepository.save(okr);
        if (keyResults != null) {
            for (HrOkrKeyResult kr : keyResults) {
                kr.setOkr(saved);
                keyResultRepository.save(kr);
            }
        }
        return saved;
    }

    @Transactional
    public HrOkrKeyResult updateKeyResult(Long keyResultId, Integer progressPercent, String currentValue) {
        HrOkrKeyResult kr = keyResultRepository.findById(keyResultId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Key result not found"));
        kr.setProgressPercent(progressPercent);
        if (currentValue != null) kr.setCurrentValue(currentValue);
        HrOkrKeyResult saved = keyResultRepository.save(kr);
        List<HrOkrKeyResult> allKrs = keyResultRepository.findByOkrId(kr.getOkr().getId());
        int avg = allKrs.stream().mapToInt(r -> r.getProgressPercent() != null ? r.getProgressPercent() : 0).sum()
            / Math.max(1, allKrs.size());
        kr.getOkr().setOverallProgress(avg);
        okrRepository.save(kr.getOkr());
        return saved;
    }

    public List<HrOkr> getEmployeeOkrs(Long employeeId) {
        return okrRepository.findByEmployeeId(employeeId);
    }

    public List<HrOkr> getOrgOkrs(Long orgId, Integer year, String quarter) {
        if (year != null && quarter != null) return okrRepository.findByOrganizationIdAndYearAndQuarter(orgId, year, quarter);
        return okrRepository.findByOrganizationId(orgId);
    }

    public List<HrOkrKeyResult> getKeyResults(Long okrId) {
        return keyResultRepository.findByOkrId(okrId);
    }

    @Transactional
    public HrFeedbackRequest createFeedbackRequest(Long orgId, Long subjectEmployeeId, Long cycleId,
                                                    LocalDate deadline, Boolean isAnonymous, Long createdBy) {
        HrEmployee subject = employeeRepository.findById(subjectEmployeeId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Employee not found"));
        HrFeedbackRequest req = HrFeedbackRequest.builder()
            .organizationId(orgId)
            .subjectEmployee(subject)
            .deadline(deadline)
            .isAnonymous(isAnonymous != null ? isAnonymous : true)
            .createdBy(createdBy)
            .build();
        if (cycleId != null) {
            cycleRepository.findById(cycleId).ifPresent(req::setAppraisalCycle);
        }
        return feedbackRequestRepository.save(req);
    }

    @Transactional
    public HrFeedbackResponse submitFeedback(Long requestId, Long reviewerEmployeeId, HrFeedbackResponse response) {
        HrFeedbackRequest request = feedbackRequestRepository.findById(requestId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Feedback request not found"));
        if (feedbackResponseRepository.existsByFeedbackRequestIdAndReviewerEmployeeId(requestId, reviewerEmployeeId)) {
            throw new IllegalStateException("You have already submitted feedback for this request");
        }
        HrEmployee reviewer = employeeRepository.findById(reviewerEmployeeId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Reviewer not found"));
        response.setFeedbackRequest(request);
        response.setReviewer(reviewer);
        response.setSubmittedAt(LocalDateTime.now());
        return feedbackResponseRepository.save(response);
    }

    public List<HrFeedbackResponse> getFeedbackResponses(Long requestId) {
        return feedbackResponseRepository.findByFeedbackRequestId(requestId);
    }
}
