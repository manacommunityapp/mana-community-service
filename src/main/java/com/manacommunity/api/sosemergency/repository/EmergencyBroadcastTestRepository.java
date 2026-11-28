package com.manacommunity.api.sosemergency.repository;

import com.manacommunity.api.sosemergency.entity.EmergencyBroadcastTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmergencyBroadcastTestRepository extends JpaRepository<EmergencyBroadcastTest, Long> {}
