package com.manacommunity.api.repository;

import com.manacommunity.api.model.CposCrmActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CposCrmActivityRepository extends JpaRepository<CposCrmActivity, Long> {
    List<CposCrmActivity> findByLeadIdOrderByCreatedAtDesc(Long leadId);
}
