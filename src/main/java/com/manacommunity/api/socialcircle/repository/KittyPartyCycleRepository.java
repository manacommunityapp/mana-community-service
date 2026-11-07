package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KittyPartyCycleRepository extends JpaRepository<KittyPartyCycle, Long> {

    List<KittyPartyCycle> findByCircleIdOrderByStartDateDesc(Long circleId);

    Optional<KittyPartyCycle> findFirstByCircleIdAndStatusOrderByStartDateDesc(Long circleId, String status);
}
