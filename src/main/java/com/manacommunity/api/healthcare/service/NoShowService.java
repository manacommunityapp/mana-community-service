package com.manacommunity.api.healthcare.service;

import com.manacommunity.api.healthcare.entity.AppointmentNoShow;
import com.manacommunity.api.healthcare.repository.AppointmentNoShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoShowService {

    private final AppointmentNoShowRepository repo;

    @Transactional
    public AppointmentNoShow recordNoShow(Long communityId, Long appointmentId, Long doctorId,
                                          Long patientUserId, BigDecimal feeCharged) {
        AppointmentNoShow ns = new AppointmentNoShow();
        ns.setCommunityId(communityId);
        ns.setAppointmentId(appointmentId);
        ns.setDoctorId(doctorId);
        ns.setPatientUserId(patientUserId);
        ns.setNoShowAt(Instant.now());
        ns.setFeeCharged(feeCharged);
        return repo.save(ns);
    }

    @Transactional
    public AppointmentNoShow waiveFee(Long id, Long reviewedBy, String reason) {
        AppointmentNoShow ns = repo.findById(id).orElseThrow();
        ns.setFeeWaived(true);
        ns.setWaiverReason(reason);
        ns.setReviewedBy(reviewedBy);
        return repo.save(ns);
    }

    public List<AppointmentNoShow> getByCommunity(Long communityId) {
        return repo.findByCommunityIdOrderByNoShowAtDesc(communityId);
    }

    public int countByPatient(Long communityId, Long patientUserId) {
        return repo.countByCommunityIdAndPatientUserId(communityId, patientUserId);
    }
}
