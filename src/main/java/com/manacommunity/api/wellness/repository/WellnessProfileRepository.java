package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.WellnessProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WellnessProfileRepository extends JpaRepository<WellnessProfile, Long> {
    Optional<WellnessProfile> findByUserId(Long userId);
}
