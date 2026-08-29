package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventBookingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventBookingRegistrationRepository extends JpaRepository<EventBookingRegistration, Long> {

    List<EventBookingRegistration> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<EventBookingRegistration> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    List<EventBookingRegistration> findByUserIdAndStatusNotOrderByCreatedAtDesc(Long userId, String status);

    List<EventBookingRegistration> findByCommunityId(Long communityId);

    List<EventBookingRegistration> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<EventBookingRegistration> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, String status);

    List<EventBookingRegistration> findByCommunityIdAndStatusNotOrderByCreatedAtDesc(Long communityId, String status);

    Optional<EventBookingRegistration> findByRegCode(String regCode);

    Optional<EventBookingRegistration> findByIdAndUserId(Long id, Long userId);

    List<EventBookingRegistration> findByActivityId(String activityId);

    boolean existsByActivityIdAndStatusNot(String activityId, String status);

    long countByActivityIdAndStatusNot(String activityId, String status);

    List<EventBookingRegistration> findByActivityTitle(String activityTitle);

    long countByCommunityId(Long communityId);

    /** True when the user already has a non-cancelled booking for the given activityId. */
    boolean existsByUserIdAndActivityIdAndStatusNot(Long userId, String activityId, String status);

    boolean existsByUserIdAndActivityIdInAndStatusNot(Long userId, Collection<String> activityIds, String status);

    List<EventBookingRegistration> findByMainEventIdOrderByCreatedAtDesc(Long mainEventId);

    long countByMainEventIdAndStatusNot(Long mainEventId, String status);

    long countByUserId(Long userId);
}

