package com.manacommunity.api.serviceplatform.interiors.repository;

import com.manacommunity.api.serviceplatform.interiors.entity.InteriorSavedInspiration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InteriorSavedInspirationRepository extends JpaRepository<InteriorSavedInspiration, Long> {
    List<InteriorSavedInspiration> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<InteriorSavedInspiration> findByIdAndUserId(Long id, Long userId);
    Optional<InteriorSavedInspiration> findByUserIdAndSourceUrl(Long userId, String sourceUrl);
}
