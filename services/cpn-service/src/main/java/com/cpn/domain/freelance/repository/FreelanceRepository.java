package com.cpn.domain.freelance.repository;

import com.cpn.domain.freelance.model.FreelanceProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FreelanceRepository extends JpaRepository<FreelanceProject, UUID> {
    List<FreelanceProject> findByStatus(String status);
    List<FreelanceProject> findByClientId(UUID clientId);
}
