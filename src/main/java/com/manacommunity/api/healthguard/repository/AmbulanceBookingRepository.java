package com.manacommunity.api.healthguard.repository;

import com.manacommunity.api.healthguard.entity.AmbulanceBooking;
import com.manacommunity.api.healthguard.enums.HealthGuardEnums.AmbulanceBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AmbulanceBookingRepository extends JpaRepository<AmbulanceBooking, Long> {
    Optional<AmbulanceBooking> findTopByUserIdAndStatusInOrderByRequestedAtDesc(Long userId, List<AmbulanceBookingStatus> statuses);
    List<AmbulanceBooking> findByUserIdOrderByRequestedAtDesc(Long userId);
    Optional<AmbulanceBooking> findByIdAndUserId(Long id, Long userId);
}
