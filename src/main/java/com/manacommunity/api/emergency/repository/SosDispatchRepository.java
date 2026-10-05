package com.manacommunity.api.emergency.repository;

import com.manacommunity.api.emergency.entity.SosDispatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SosDispatchRepository extends JpaRepository<SosDispatch, Long> {

    List<SosDispatch> findByIncidentId(Long incidentId);

    List<SosDispatch> findByGuardIdOrderByDispatchedAtDesc(Long guardId);
}