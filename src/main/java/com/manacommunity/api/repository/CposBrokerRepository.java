package com.manacommunity.api.repository;

import com.manacommunity.api.model.CposBroker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CposBrokerRepository extends JpaRepository<CposBroker, Long> {
    List<CposBroker> findByCommunityIdAndActiveTrueOrderByBrokerNameAsc(Long communityId);
}
