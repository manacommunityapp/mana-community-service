package com.manacommunity.api.academy.session.service;

import com.manacommunity.api.academy.session.entity.AcademySession;
import com.manacommunity.api.academy.session.entity.AcademySessionEnrollment;
import com.manacommunity.api.academy.session.repository.AcademySessionEnrollmentRepository;
import com.manacommunity.api.academy.session.repository.AcademySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AcademySessionService {

    private final AcademySessionRepository sessionRepo;
    private final AcademySessionEnrollmentRepository enrollmentRepo;

    public AcademySession createSession(AcademySession session) {
        session.setStatus("DRAFT");
        session.setEnrolledCount(0);
        return sessionRepo.save(session);
    }

    public AcademySession publishSession(Long sessionId, Long instructorId) {
        AcademySession session = findOwnedSession(sessionId, instructorId);
        if (!"DRAFT".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only DRAFT sessions can be published");
        }
        session.setStatus("PUBLISHED");
        return sessionRepo.save(session);
    }

    public AcademySession cancelSession(Long sessionId, Long instructorId) {
        AcademySession session = findOwnedSession(sessionId, instructorId);
        session.setStatus("CANCELLED");
        return sessionRepo.save(session);
    }

    @Transactional(readOnly = true)
    public Page<AcademySession> getUpcomingSessions(Long communityId, String category, Pageable pageable) {
        List<String> activeStatuses = List.of("PUBLISHED", "FULL");
        if (category != null && !category.isBlank()) {
            return sessionRepo.findByCommunityIdAndCategoryAndStatusInOrderByStartsAtAsc(
                    communityId, category, activeStatuses, pageable);
        }
        return sessionRepo.findByCommunityIdAndStatusInOrderByStartsAtAsc(communityId, activeStatuses, pageable);
    }

    @Transactional(readOnly = true)
    public List<AcademySession> getMyTeaching(Long instructorId) {
        return sessionRepo.findByInstructorIdOrderByStartsAtDesc(instructorId);
    }

    @Transactional(readOnly = true)
    public List<AcademySessionEnrollment> getMyEnrollments(Long studentId) {
        return enrollmentRepo.findByStudentIdOrderByEnrolledAtDesc(studentId);
    }

    public AcademySessionEnrollment enroll(Long sessionId, Long studentId) {
        AcademySession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        if (!"PUBLISHED".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is not open for enrollment");
        }
        if (session.getMaxSeats() != null && session.getEnrolledCount() >= session.getMaxSeats()) {
            session.setStatus("FULL");
            sessionRepo.save(session);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is full");
        }
        if (enrollmentRepo.existsBySessionIdAndStudentId(sessionId, studentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled");
        }
        try {
            String paymentStatus = session.getPrice().compareTo(java.math.BigDecimal.ZERO) == 0 ? "FREE" : "PENDING";
            AcademySessionEnrollment enrollment = enrollmentRepo.save(AcademySessionEnrollment.builder()
                    .sessionId(sessionId)
                    .studentId(studentId)
                    .paymentStatus(paymentStatus)
                    .build());
            session.setEnrolledCount(session.getEnrolledCount() + 1);
            if (session.getMaxSeats() != null && session.getEnrolledCount() >= session.getMaxSeats()) {
                session.setStatus("FULL");
            }
            sessionRepo.save(session);
            return enrollment;
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled");
        }
    }

    public void cancelEnrollment(Long enrollmentId, Long studentId) {
        AcademySessionEnrollment enrollment = enrollmentRepo.findById(enrollmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));
        if (!enrollment.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your enrollment");
        }
        enrollmentRepo.delete(enrollment);
        sessionRepo.findById(enrollment.getSessionId()).ifPresent(s -> {
            s.setEnrolledCount(Math.max(0, s.getEnrolledCount() - 1));
            if ("FULL".equals(s.getStatus())) s.setStatus("PUBLISHED");
            sessionRepo.save(s);
        });
    }

    public void submitFeedback(Long enrollmentId, Long studentId, int rating, String text) {
        AcademySessionEnrollment enrollment = enrollmentRepo.findById(enrollmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));
        if (!enrollment.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your enrollment");
        }
        enrollment.setFeedbackRating((short) Math.max(1, Math.min(5, rating)));
        enrollment.setFeedbackText(text);
        enrollmentRepo.save(enrollment);
    }

    private AcademySession findOwnedSession(Long sessionId, Long instructorId) {
        AcademySession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        if (!session.getInstructorId().equals(instructorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your session");
        }
        return session;
    }
}
