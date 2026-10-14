package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthMedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HealthMedicalRecordRepository extends JpaRepository<HealthMedicalRecord, Long> {
    List<HealthMedicalRecord> findByTenantIdAndPatientIdOrderByRecordDateDesc(Long tenantId, UUID patientId);
}