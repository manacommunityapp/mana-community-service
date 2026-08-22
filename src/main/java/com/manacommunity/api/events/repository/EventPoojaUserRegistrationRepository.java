package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventPoojaUserRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventPoojaUserRegistrationRepository extends JpaRepository<EventPoojaUserRegistration, Long> {

    List<EventPoojaUserRegistration> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<EventPoojaUserRegistration> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<EventPoojaUserRegistration> findByEventIdOrderByCreatedAtDesc(Long eventId);

    Optional<EventPoojaUserRegistration> findByRegCode(String regCode);

    Optional<EventPoojaUserRegistration> findByIdAndUserId(Long id, Long userId);

    long countByEventIdAndStatusNot(Long eventId, String status);

    boolean existsByUserIdAndEventIdAndPoojaSlotDateAndStatusNot(Long userId, Long eventId, String slotDate, String status);
}
