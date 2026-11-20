package com.manacommunity.api.healthcare.service;

import com.manacommunity.api.healthcare.entity.Referral;
import com.manacommunity.api.healthcare.repository.ReferralRepository;
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

    public List<Referral> getByCommunity(Long communityId) {
        return repo.findByCommunityIdOrderByCreatedAtDesc(communityId);
    }

    public List<Referral> getByPatient(Long communityId, Long patientUserId) {
        return repo.findByCommunityIdAndPatientUserId(communityId, patientUserId);
    }

    public List<Referral> getPendingReferrals(Long communityId) {
        return repo.findByCommunityIdAndStatus(communityId, "PENDING");
    }
}
