package com.manacommunity.api.healthcare.repository;

import com.manacommunity.api.healthcare.entity.AppointmentNoShow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentNoShowRepository extends JpaRepository<AppointmentNoShow, Long> {
    List<AppointmentNoShow> findByCommunityIdOrderByNoShowAtDesc(Long communityId);
    List<AppointmentNoShow> findByCommunityIdAndDoctorId(Long communityId, Long doctorId);
    List<AppointmentNoShow> findByCommunityIdAndPatientUserId(Long communityId, Long patientUserId);
    int countByCommunityIdAndPatientUserId(Long communityId, Long patientUserId);
}
