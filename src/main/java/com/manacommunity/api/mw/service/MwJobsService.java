package com.manacommunity.api.mw.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.mw.dto.*;
import com.manacommunity.api.mw.entity.MwJobApplication;
import com.manacommunity.api.mw.entity.MwJobListing;
import com.manacommunity.api.mw.repository.MwJobApplicationRepository;
import com.manacommunity.api.mw.repository.MwJobListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MwJobsService {

    private final MwJobListingRepository jobRepo;
    private final MwJobApplicationRepository appRepo;

    public List<MwJobListing> getCommunityJobs(String search) {
        LocalDate today = LocalDate.now();
        if (search != null && !search.isBlank()) {
            return jobRepo.searchActiveCommunityListings(today, search.toLowerCase());
        }
        return jobRepo.findActiveCommunityListings(today);
    }

    public MwJobListing createJobListing(Long orgId, Long createdBy, CreateJobListingRequest req) {
        return jobRepo.save(MwJobListing.builder()
                .organizationId(orgId)
                .createdBy(createdBy)
                .title(req.getTitle())
                .description(req.getDescription())
                .employmentType(req.getEmploymentType())
                .location(req.getLocation())
                .isRemote(req.getIsRemote())
                .salaryMin(req.getSalaryMin())
                .salaryMax(req.getSalaryMax())
                .requiredSkills(req.getRequiredSkills())
                .experienceYears(req.getExperienceYears())
                .expiresAt(req.getExpiresAt())
                .build());
    }

    public List<MwJobListing> getOrgListings(Long orgId) {
        return jobRepo.findByOrganizationIdOrderByCreatedAtDesc(orgId);
    }

    public MwJobListing pauseListing(Long orgId, Long jobId) {
        MwJobListing job = getAndVerifyOwnership(orgId, jobId);
        job.setStatus("PAUSED");
        return jobRepo.save(job);
    }

    public MwJobListing closeListing(Long orgId, Long jobId) {
        MwJobListing job = getAndVerifyOwnership(orgId, jobId);
        job.setStatus("CLOSED");
        return jobRepo.save(job);
    }

    public MwJobApplication applyToJob(Long jobId, Long manaUserId, ApplyToJobRequest req) {
        MwJobListing job = jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job listing not found"));
        if (!"ACTIVE".equals(job.getStatus())) {
            throw new IllegalStateException("Job is not accepting applications");
        }
        if (appRepo.findByJob_IdAndManaUserId(jobId, manaUserId).isPresent()) {
            throw new IllegalStateException("Already applied to this job");
        }
        return appRepo.save(MwJobApplication.builder()
                .job(job)
                .manaUserId(manaUserId)
                .passportId(req.getPassportId())
                .coverNote(req.getCoverNote())
                .build());
    }

    public List<MwJobApplication> getJobApplications(Long orgId, Long jobId) {
        getAndVerifyOwnership(orgId, jobId);
        return appRepo.findByJob_Id(jobId);
    }

    public MwJobApplication respondToApplication(Long orgId, Long jobId, Long appId, JobApplicationResponseRequest req) {
        getAndVerifyOwnership(orgId, jobId);
        MwJobApplication app = appRepo.findById(appId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        app.setStatus(req.getStatus());
        app.setEmployerNote(req.getEmployerNote());
        app.setRespondedAt(LocalDateTime.now());
        return appRepo.save(app);
    }

    public List<MwJobApplication> getMyApplications(Long manaUserId) {
        return appRepo.findByManaUserId(manaUserId);
    }

    private MwJobListing getAndVerifyOwnership(Long orgId, Long jobId) {
        MwJobListing job = jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job listing not found"));
        if (!job.getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("Not your listing");
        }
        return job;
    }
}
