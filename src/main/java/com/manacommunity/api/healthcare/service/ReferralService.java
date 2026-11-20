package com.manacommunity.api.healthcare.service;

import com.manacommunity.api.healthcare.entity.Referral;
import com.manacommunity.api.healthcare.repository.ReferralRepository;
import com.manacommunity.platform.privacy.HealthDataAccess;
import com.manacommunity.platform.privacy.HealthDataIsolationGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralRepository repo;

    @Transactional
    public Referral create(Referral referral) {
        return repo.save(referral);
    }

    @Transactional
    public Referral markAppointmentBooked(Long id, LocalDate appointmentDate) {
        Referral r = repo.findById(id).orElseThrow();
        r.setAppointmentBooked(true);
        r.setAppointmentDate(appointmentDate);
        r.setStatus("APPOINTMENT_BOOKED");
        r.setUpdatedAt(Instant.now());
        return repo.save(r);
    }

    @Transactional
    public Referral updateOutcome(Long id, String outcomeNotes, String feedbackToReferrer, String status) {
        Referral r = repo.findById(id).orElseThrow();
        r.setOutcomeNotes(outcomeNotes);
        r.setFeedbackToReferrer(feedbackToReferrer);
        r.setStatus(status);
        r.setUpdatedAt(Instant.now());
        return repo.save(r);
    }

    @HealthDataAccess(recordType = "REFERRAL", purpose = "COMMUNITY_VIEW")
    public List<Referral> getByCommunity(Long communityId) {
        HealthDataIsolationGuard.assertNotCommunityFeedContext();
        return repo.findByCommunityIdOrderByCreatedAtDesc(communityId);
    }

    @HealthDataAccess(recordType = "REFERRAL", purpose = "PATIENT_VIEW")
    public List<Referral> getByPatient(Long communityId, Long patientUserId) {
        HealthDataIsolationGuard.assertNotCommunityFeedContext();
        return repo.findByCommunityIdAndPatientUserId(communityId, patientUserId);
    }

    @HealthDataAccess(recordType = "REFERRAL", purpose = "PENDING_VIEW")
    public List<Referral> getPendingReferrals(Long communityId) {
        HealthDataIsolationGuard.assertNotCommunityFeedContext();
        return repo.findByCommunityIdAndStatus(communityId, "PENDING");
    }
}
