package com.manacommunity.api.repository;

import com.manacommunity.api.model.DiningRsvp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiningRsvpRepository extends JpaRepository<DiningRsvp, Long> {

    List<DiningRsvp> findByDiningEventIdOrderByCreatedAtDesc(Long diningEventId);

    Optional<DiningRsvp> findByIdAndUserId(Long id, Long userId);

    boolean existsByDiningEventIdAndUserId(Long diningEventId, Long userId);
}
