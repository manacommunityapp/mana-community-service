package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrLearningService {
    private final HrTrainingProgramRepository trainingProgramRepository;
    private final HrTrainingEnrollmentRepository enrollmentRepository;
    private final HrCertificationRepository certificationRepository;
    private final HrCareerPathRepository careerPathRepository;
    private final HrRecognitionRepository recognitionRepository;
    private final HrEmployeeRepository employeeRepository;

    @Transactional
    public HrTrainingProgram createProgram(Long orgId, HrTrainingProgram program) {
        program.setOrganizationId(orgId);
        return trainingProgramRepository.save(program);
    }

    public Page<HrTrainingProgram> getPrograms(Long orgId, String status, Pageable pageable) {
        if (status != null) return trainingProgramRepository.findByOrganizationIdAndStatus(orgId, status, pageable);
        return trainingProgramRepository.findByOrganizationId(orgId, pageable);
    }

    @Transactional
    public HrTrainingEnrollment enroll(Long orgId, Long employeeId, Long programId) {
        if (enrollmentRepository.existsByEmployeeIdAndTrainingProgramId(employeeId, programId)) {
            throw new IllegalStateException("Employee is already enrolled in this training");
        }
        HrEmployee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Employee not found"));
        HrTrainingProgram program = trainingProgramRepository.findById(programId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Training program not found"));
        return enrollmentRepository.save(HrTrainingEnrollment.builder()
            .organizationId(orgId)
            .employee(employee)
            .trainingProgram(program)
            .enrolledDate(LocalDate.now())
            .build());
    }

    @Transactional
    public HrTrainingEnrollment completeEnrollment(Long enrollmentId, Integer score, String feedback) {
        HrTrainingEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Enrollment not found"));
        enrollment.setStatus("COMPLETED");
        enrollment.setCompletionDate(LocalDate.now());
        enrollment.setScore(score);
        enrollment.setFeedback(feedback);
        return enrollmentRepository.save(enrollment);
    }

    public List<HrTrainingEnrollment> getEmployeeEnrollments(Long employeeId) {
        return enrollmentRepository.findByEmployeeId(employeeId);
    }

    @Transactional
    public HrCertification addCertification(Long orgId, Long employeeId, HrCertification cert) {
        HrEmployee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Employee not found"));
        cert.setOrganizationId(orgId);
        cert.setEmployee(employee);
        return certificationRepository.save(cert);
    }

    public List<HrCertification> getEmployeeCertifications(Long employeeId) {
        return certificationRepository.findByEmployeeId(employeeId);
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendCertificationExpiryReminders() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysOut = today.plusDays(30);
        // Pass null for orgId to match all orgs — repository handles null by ignoring that filter
        List<HrCertification> expiring = certificationRepository
            .findByOrganizationIdAndExpiryDateBetweenAndReminderSentFalse(null, today, thirtyDaysOut);
        for (HrCertification cert : expiring) {
            cert.setReminderSent(true);
            certificationRepository.save(cert);
        }
    }

    @Transactional
    public HrCareerPath saveCareerPath(Long orgId, Long employeeId, HrCareerPath path) {
        HrEmployee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Employee not found"));
        careerPathRepository.findByEmployeeIdAndStatus(employeeId, "ACTIVE")
            .ifPresent(existing -> {
                existing.setStatus("SUPERSEDED");
                careerPathRepository.save(existing);
            });
        path.setOrganizationId(orgId);
        path.setEmployee(employee);
        return careerPathRepository.save(path);
    }

    public HrCareerPath getActiveCareerPath(Long employeeId) {
        return careerPathRepository.findByEmployeeIdAndStatus(employeeId, "ACTIVE").orElse(null);
    }

    @Transactional
    public HrRecognition giveRecognition(Long orgId, Long giverEmployeeId, Long receiverEmployeeId,
                                          String type, String message, Integer points, Boolean isPublic, String category) {
        HrEmployee giver = employeeRepository.findById(giverEmployeeId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Giver employee not found"));
        HrEmployee receiver = employeeRepository.findById(receiverEmployeeId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Receiver employee not found"));
        return recognitionRepository.save(HrRecognition.builder()
            .organizationId(orgId)
            .giverEmployee(giver)
            .receiverEmployee(receiver)
            .recognitionType(type != null ? type : "KUDOS")
            .message(message)
            .points(points != null ? points : 0)
            .isPublic(isPublic != null ? isPublic : true)
            .category(category)
            .build());
    }

    public Page<HrRecognition> getPublicRecognitions(Long orgId, Pageable pageable) {
        return recognitionRepository.findByOrganizationIdAndIsPublicTrue(orgId, pageable);
    }

    public Page<HrRecognition> getMyRecognitions(Long employeeId, Pageable pageable) {
        return recognitionRepository.findByReceiverEmployeeId(employeeId, pageable);
    }
}
