package com.manacommunity.api.notification.repository;

import com.manacommunity.api.notification.entity.UserSmsPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSmsPreferenceRepository extends JpaRepository<UserSmsPreference, Long> {

    List<UserSmsPreference> findByUserId(Long userId);

    Optional<UserSmsPreference> findByUserIdAndNotificationType(Long userId, String notificationType);

    boolean existsByUserIdAndNotificationTypeAndSmsEnabledTrue(Long userId, String notificationType);
}
