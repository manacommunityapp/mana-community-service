package com.manacommunity.api.healthcare.repository;

import com.manacommunity.api.healthcare.entity.DoctorSlotUtilization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DoctorSlotUtilizationRepository extends JpaRepository<DoctorSlotUtilization, Long> {
    Optional<DoctorSlotUtilization> findByCommunityIdAndDoctorIdAndSlotDate(Long communityId, Long doctorId, LocalDate slotDate);
    List<DoctorSlotUtilization> findByCommunityIdAndDoctorIdAndSlotDateBetweenOrderBySlotDateAsc(Long communityId, Long doctorId, LocalDate from, LocalDate to);
    List<DoctorSlotUtilization> findByCommunityIdAndSlotDateOrderByDoctorIdAsc(Long communityId, LocalDate slotDate);
}
