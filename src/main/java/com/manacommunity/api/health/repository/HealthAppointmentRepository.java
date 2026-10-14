package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HealthAppointmentRepository extends JpaRepository<HealthAppointment, Long> {
    List<HealthAppointment> findByTenantIdAndDoctorIdAndSlotDate(Long tenantId, Long doctorId, LocalDate slotDate);
    List<HealthAppointment> findByTenantIdAndBookedByUserIdOrderBySlotDateDescSlotStartTimeDesc(Long tenantId, Long userId);
    Optional<HealthAppointment> findByAppointmentUuid(UUID uuid);
}