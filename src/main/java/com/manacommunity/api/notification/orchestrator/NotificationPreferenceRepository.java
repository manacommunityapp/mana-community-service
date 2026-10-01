package com.manacommunity.api.notification.orchestrator;

import com.manacommunity.api.notification.orchestrator.NotificationEnums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    List<NotificationPreference> findByUserId(Long userId);
    Optional<NotificationPreference> findByUserIdAndCategoryAndChannel(Long userId, NotificationCategory category, NotificationChannel channel);
    List<NotificationPreference> findByUserIdAndCategory(Long userId, NotificationCategory category);
}
