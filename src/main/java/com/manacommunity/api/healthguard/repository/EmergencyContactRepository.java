package com.manacommunity.api.healthguard.repository;

import com.manacommunity.api.healthguard.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {
    List<EmergencyContact> findByUserIdAndIsActiveTrueOrderByPriorityAsc(Long userId);
    Optional<EmergencyContact> findByIdAndUserId(Long id, Long userId);
    List<EmergencyContact> findByUserIdAndNotifyOnSosTrueAndIsActiveTrueOrderByPriorityAsc(Long userId);
}
