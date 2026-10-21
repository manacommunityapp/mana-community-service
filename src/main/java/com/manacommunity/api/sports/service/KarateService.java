package com.manacommunity.api.sports.service;

import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface KarateService {

    // Belts
    List<KarateBelt> listBelts();

    // Programs
    KarateProgram createProgram(KarateProgramRequest req, AppUser admin);
    List<KarateProgram> listPrograms(AppUser user);
    KarateProgram getProgram(Long id, AppUser user);

    // Batches
    KarateBatch createBatch(Long programId, KarateBatchRequest req, AppUser admin);
    List<KarateBatch> listBatches(Long programId, AppUser user);
    KarateBatch updateBatchStatus(Long batchId, String status, AppUser admin);

    // Enrollment
    KarateEnrollment enroll(Long batchId, KarateEnrollRequest req, AppUser user);
    KarateAttendanceResponse getAttendance(Long batchId, AppUser admin);

    // Exams
    KarateExam createExam(Long batchId, KarateExamRequest req, AppUser admin);
    List<KarateExam> listExams(Long batchId, AppUser user);
    KarateExamResult recordResult(Long examId, KarateExamResultRequest req, AppUser admin);
    List<KarateExamResult> listResults(Long examId, AppUser admin);
}
