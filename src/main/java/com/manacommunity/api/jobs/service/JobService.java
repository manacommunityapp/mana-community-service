package com.manacommunity.api.jobs.service;

import com.manacommunity.api.jobs.dto.ApplicationRequest;
import com.manacommunity.api.jobs.dto.JobRequest;
import com.manacommunity.api.jobs.dto.JobResponse;
import com.manacommunity.api.jobs.entity.Job;
import com.manacommunity.api.jobs.entity.JobApplication;
import com.manacommunity.api.jobs.repository.JobApplicationRepository;
import com.manacommunity.api.jobs.repository.JobRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepo;
    private final JobApplicationRepository appRepo;

    @Transactional(readOnly = true)
    public List<JobResponse> getActiveJobs(Long communityId, String query, Long currentUserId) {
        List<Job> jobs;
        if (query != null && !query.isBlank()) {
            jobs = jobRepo.searchActiveByCommunity(communityId, query.trim());
        } else {
            jobs = jobRepo.findActiveByCommunity(communityId);
        }
        return jobs.stream().map(j -> toResponse(j, currentUserId)).toList();
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getAllJobs(Long communityId, Long currentUserId) {
        return jobRepo.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream().map(j -> toResponse(j, currentUserId)).toList();
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getMyJobs(Long userId) {
        return jobRepo.findByPostedByIdOrderByCreatedAtDesc(userId)
                .stream().map(j -> toResponse(j, userId)).toList();
    }

    @Transactional(readOnly = true)
    public JobResponse getById(Long id, Long currentUserId) {
        Job job = jobRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
        return toResponse(job, currentUserId);
    }

    @Transactional
    public JobResponse create(JobRequest req, AppUser user, Community community) {
        Job job = Job.builder()
                .title(req.getTitle())
                .company(req.getCompany())
                .description(req.getDescription())
                .location(req.getLocation())
                .jobType(parseEnum(Job.JobType.class, req.getJobType(), Job.JobType.FULL_TIME))
                .salary(req.getSalary())
                .referral(req.isReferral())
                .contactEmail(req.getContactEmail())
                .postedBy(user)
                .community(community)
                .build();
        return toResponse(jobRepo.save(job), user.getId());
    }

    @Transactional
    public JobResponse update(Long id, JobRequest req, Long userId) {
        Job job = jobRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
        if (!job.getPostedBy().getId().equals(userId)) {
            throw new IllegalStateException("Only the poster can edit this job");
        }
        job.setTitle(req.getTitle());
        job.setCompany(req.getCompany());
        job.setDescription(req.getDescription());
        job.setLocation(req.getLocation());
        job.setJobType(parseEnum(Job.JobType.class, req.getJobType(), job.getJobType()));
        job.setSalary(req.getSalary());
        job.setReferral(req.isReferral());
        job.setContactEmail(req.getContactEmail());
        return toResponse(jobRepo.save(job), userId);
    }

    @Transactional
    public JobResponse closeJob(Long id, Long userId) {
        Job job = jobRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
        if (!job.getPostedBy().getId().equals(userId)) {
            throw new IllegalStateException("Only the poster can close this job");
        }
        job.setStatus(Job.JobStatus.CLOSED);
        return toResponse(jobRepo.save(job), userId);
    }

    @Transactional
    public JobResponse apply(Long jobId, ApplicationRequest req, AppUser applicant) {
        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        if (job.getStatus() != Job.JobStatus.ACTIVE) {
            throw new IllegalStateException("This job is no longer accepting applications");
        }
        if (appRepo.existsByJobIdAndApplicantId(jobId, applicant.getId())) {
            throw new IllegalStateException("You have already applied for this job");
        }
        JobApplication application = JobApplication.builder()
                .job(job)
                .applicant(applicant)
                .coverNote(req != null ? req.getCoverNote() : null)
                .resumeUrl(req != null ? req.getResumeUrl() : null)
                .build();
        appRepo.save(application);
        return toResponse(jobRepo.findById(jobId).orElseThrow(), applicant.getId());
    }

    private JobResponse toResponse(Job j, Long currentUserId) {
        boolean hasApplied = currentUserId != null && appRepo.existsByJobIdAndApplicantId(j.getId(), currentUserId);
        return JobResponse.builder()
                .id(j.getId())
                .title(j.getTitle())
                .company(j.getCompany())
                .description(j.getDescription())
                .location(j.getLocation())
                .jobType(j.getJobType().name())
                .salary(j.getSalary())
                .referral(j.isReferral())
                .contactEmail(j.getContactEmail())
                .status(j.getStatus().name())
                .postedById(j.getPostedBy().getId())
                .postedByName(j.getPostedBy().getFullName())
                .communityId(j.getCommunity() != null ? j.getCommunity().getId() : null)
                .applicationCount(j.getApplications() != null ? j.getApplications().size() : 0)
                .hasApplied(hasApplied)
                .createdAt(formatDt(j.getCreatedAt()))
                .build();
    }

    private String formatDt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, E def) {
        if (value == null || value.isBlank()) return def;
        try { return Enum.valueOf(enumClass, value); }
        catch (IllegalArgumentException e) { return def; }
    }
}
