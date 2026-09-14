package com.manacommunity.api.service;

import com.manacommunity.api.dto.karate.*;
import com.manacommunity.api.model.karate.*;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SportsKarateService {

    // ── Belts ──────────────────────────────────────────────────────────────

    List<SportsKarateBelt> getBelts(AppUser caller);

    SportsKarateBelt createBelt(KarateBeltRequest req, AppUser caller);

    SportsKarateBelt updateBelt(Long id, KarateBeltRequest req);

    void deleteBelt(Long id);

    // ── Programs ───────────────────────────────────────────────────────────

    Page<SportsKarateProgram> getPrograms(AppUser caller, Pageable pageable);

    SportsKarateProgram getProgram(Long id);

    SportsKarateProgram createProgram(KarateProgramRequest req, AppUser caller);

    SportsKarateProgram updateProgram(Long id, KarateProgramRequest req);

    void deleteProgram(Long id);

    // ── Batches ────────────────────────────────────────────────────────────

    List<SportsKarateBatch> getBatches(Long programId);

    SportsKarateBatch getBatch(Long id);

    SportsKarateBatch createBatch(Long programId, KarateBatchRequest req, AppUser caller);

    SportsKarateBatch updateBatch(Long id, KarateBatchRequest req);

    SportsKarateBatch updateBatchStatus(Long id, SportsKarateBatch.BatchStatus status, AppUser caller);

    // ── Enrollments ────────────────────────────────────────────────────────

    SportsKarateEnrollment enroll(Long batchId, KarateEnrollRequest req, AppUser caller);

    List<SportsKarateEnrollment> getEnrollments(Long batchId);

    List<SportsKarateEnrollment> getMyEnrollments(AppUser caller);

    SportsKarateEnrollment updateEnrollmentStatus(Long id, SportsKarateEnrollment.EnrollmentStatus status);

    SportsKarateEnrollment getEnrollmentProgress(Long id);

    List<SportsKarateEnrollment> getGradingEligible(Long batchId);

    // ── Classes ────────────────────────────────────────────────────────────

    List<SportsKarateClass> generateClasses(Long batchId, KarateClassGenerateRequest req, AppUser caller);

    List<SportsKarateClass> getClasses(Long batchId);

    SportsKarateClass getClass(Long id);

    SportsKarateClass updateClass(Long id, String topic, String classNotes);

    SportsKarateClass cancelClass(Long id, String reason, AppUser caller);

    SportsKarateClass startClass(Long id, AppUser caller);

    SportsKarateClass completeClass(Long id, AppUser caller);

    // ── Attendance ─────────────────────────────────────────────────────────

    List<SportsKarateAttendance> markAttendance(Long classId, List<KarateAttendanceEntry> entries, AppUser caller);

    List<SportsKarateAttendance> getAttendanceForClass(Long classId);

    List<SportsKarateAttendance> getAttendanceForEnrollment(Long enrollmentId);

    SportsKarateAttendance updateAttendance(Long attendanceId, SportsKarateAttendance.AttendanceStatus status, String notes);

    // ── Grading Exams ──────────────────────────────────────────────────────

    SportsKarateGradingExam scheduleExam(Long batchId, KarateGradingExamRequest req, AppUser caller);

    List<SportsKarateGradingExam> getExams(Long batchId);

    List<SportsKarateEnrollment> getEligibleForExam(Long examId);

    List<SportsKarateExamResult> submitResults(Long examId, List<KarateExamResultEntry> entries, AppUser caller);

    List<SportsKarateExamResult> getExamResults(Long examId);

    SportsKarateGradingExam cancelExam(Long id, AppUser caller);
}
