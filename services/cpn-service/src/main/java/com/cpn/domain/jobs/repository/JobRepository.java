package com.cpn.domain.jobs.repository;

import com.cpn.domain.jobs.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findByCompanyId(UUID companyId);
    List<Job> findByTitleContainingIgnoreCaseOrIndustryContainingIgnoreCase(String title, String industry);
}
