package com.manacommunity.api.healthcare.service;

import com.manacommunity.api.healthcare.entity.PrescriptionFollowup;
import com.manacommunity.api.healthcare.repository.PrescriptionFollowupRepository;
import com.manacommunity.platform.privacy.HealthDataAccess;
import com.manacommunity.platform.privacy.HealthDataIsolationGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionFollowupService {

    private final PrescriptionFollowupRepository repo;

    @Transactional
    public PrescriptionFollowup create(PrescriptionFollowup followup) {
        return repo.save(followup);
    }

    @Transactional
    public PrescriptionFollowup markComplete(Long id) {
        PrescriptionFollowup f = repo.findById(id).orElseThrow();
        f.setCompleted(true);
        f.setCompletedAt(Instant.now());
        return repo.save(f);
    }

    @HealthDataAccess(recordType = "PRESCRIPTION_FOLLOWUP", purpose = "COMMUNITY_VIEW")
    public List<PrescriptionFollowup> getPendingForCommunity(Long communityId) {
        HealthDataIsolationGuard.assertNotCommunityFeedContext();
        return repo.findByCommunityIdAndCompletedFalseOrderByFollowupDueDateAsc(communityId);
    }

    @HealthDataAccess(recordType = "PRESCRIPTION_FOLLOWUP", purpose = "PATIENT_VIEW")
    public List<PrescriptionFollowup> getForPatient(Long communityId, Long patientUserId) {
        HealthDataIsolationGuard.assertNotCommunityFeedContext();
        return repo.findByCommunityIdAndPatientUserIdOrderByFollowupDueDateAsc(communityId, patientUserId);
    }

    /** Runs daily at 7am — sends reminder for follow-ups due tomorrow. */
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void sendDueReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<PrescriptionFollowup> due = repo.findDueForReminder(tomorrow);
        for (PrescriptionFollowup f : due) {
            log.info("Prescription follow-up reminder: patient={} medication={} due={}", f.getPatientUserId(), f.getMedicationName(), f.getFollowupDueDate());
            f.setReminderSent(true);
            f.setReminderSentAt(Instant.now());
            repo.save(f);
        }
    }
}
