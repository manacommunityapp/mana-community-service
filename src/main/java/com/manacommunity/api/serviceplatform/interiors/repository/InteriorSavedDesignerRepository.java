package com.manacommunity.api.serviceplatform.interiors.repository;

import com.manacommunity.api.serviceplatform.interiors.entity.InteriorSavedDesigner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InteriorSavedDesignerRepository extends JpaRepository<InteriorSavedDesigner, Long> {
    List<InteriorSavedDesigner> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndProviderId(Long userId, Long providerId);
    void deleteByUserIdAndProviderId(Long userId, Long providerId);
}
