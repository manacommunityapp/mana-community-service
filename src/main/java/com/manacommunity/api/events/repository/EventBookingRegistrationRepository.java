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

    List<EventBookingRegistration> findByCommunityId(Long communityId);

    List<EventBookingRegistration> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    Optional<EventBookingRegistration> findByRegCode(String regCode);

    Optional<EventBookingRegistration> findByIdAndUserId(Long id, Long userId);

    List<EventBookingRegistration> findByActivityId(String activityId);

    boolean existsByActivityIdAndStatusNot(String activityId, String status);

    long countByActivityIdAndStatusNot(String activityId, String status);

    List<EventBookingRegistration> findByActivityTitle(String activityTitle);

    long countByCommunityId(Long communityId);

    /** True when the user already has a non-cancelled booking for the given activityId. */
    boolean existsByUserIdAndActivityIdAndStatusNot(Long userId, String activityId, String status);

    /** True when the user already has a non-cancelled booking for any of the given activityIds. */
    boolean existsByUserIdAndActivityIdInAndStatusNot(Long userId, Collection<String> activityIds, String status);
}
