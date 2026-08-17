package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventBookingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventBookingRegistrationRepository extends JpaRepository<EventBookingRegistration, Long> {

    List<EventBookingRegistration> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<EventBookingRegistration> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    Optional<EventBookingRegistration> findByRegCode(String regCode);

    Optional<EventBookingRegistration> findByIdAndUserId(Long id, Long userId);
}
