package com.manacommunity.api.repository;

import com.manacommunity.api.model.UserHubUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserHubUsageRepository extends JpaRepository<UserHubUsage, Long> {

    Optional<UserHubUsage> findByUserIdAndHubId(Long userId, String hubId);

    List<UserHubUsage> findByUserIdOrderByClickCountDesc(Long userId);

    @Query("SELECT u FROM UserHubUsage u WHERE u.user.id = :userId ORDER BY (u.clickCount * 0.7 + TIMESTAMPDIFF(HOUR, u.lastUsedAt, CURRENT_TIMESTAMP) * -0.3) DESC")
    List<UserHubUsage> findTopHubsByScore(@Param("userId") Long userId);

    void deleteByUserIdAndHubId(Long userId, String hubId);
}
