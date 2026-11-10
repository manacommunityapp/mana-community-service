package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrRecruitmentService {
    private final HrJobPostingRepository jobPostingRepository;
    private final HrApplicantRepository applicantRepository;
    private final HrInterviewRepository interviewRepository;
    private final HrOfferRepository offerRepository;
    private final HrEmployeeRepository employeeRepository;

    @Transactional
    public HrJobPosting createJobPosting(Long orgId, HrJobPosting posting) {
        posting.setOrganizationId(orgId);
        return jobPostingRepository.save(posting);
    }

    @Transactional
    public HrJobPosting updateJobPostingStatus(Long orgId, Long postingId, String status) {
        HrJobPosting posting = jobPostingRepository.findById(postingId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Job posting not found"));
        if (!posting.getOrganizationId().equals(orgId))
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        posting.setStatus(status);
        return jobPostingRepository.save(posting);
    }

    public Page<HrJobPosting> getJobPostings(Long orgId, String status, Pageable pageable) {
        if (status != null) return jobPostingRepository.findByOrganizationIdAndStatus(orgId, status, pageable);
        return jobPostingRepository.findByOrganizationId(orgId, pageable);
    }

    @Transactional
    public HrApplicant createApplicant(Long orgId, Long jobPostingId, HrApplicant applicant) {
        HrJobPosting posting = jobPostingRepository.findById(jobPostingId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Job posting not found"));
        applicant.setOrganizationId(orgId);
        applicant.setJobPosting(posting);
        return applicantRepository.save(applicant);
    }

    @Transactional
    public HrApplicant updateApplicantStatus(Long orgId, Long applicantId, String status, String notes) {
        HrApplicant applicant = applicantRepository.findById(applicantId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Applicant not found"));
        if (!applicant.getOrganizationId().equals(orgId))
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        applicant.setStatus(status);
        if (notes != null) applicant.setNotes(notes);
        return applicantRepository.save(applicant);
    }

    public Page<HrApplicant> getApplicants(Long jobPostingId, String status, Pageable pageable) {
        if (status != null) return applicantRepository.findByJobPostingIdAndStatus(jobPostingId, status, pageable);
        return applicantRepository.findByJobPostingId(jobPostingId, pageable);
    }

    @Transactional
    public HrInterview scheduleInterview(Long orgId, Long applicantId, HrInterview interview) {
        HrApplicant applicant = applicantRepository.findById(applicantId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Applicant not found"));
        if (!applicant.getOrganizationId().equals(orgId))
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        interview.setOrganizationId(orgId);
        interview.setApplicant(applicant);
        if (interview.getInterviewer() != null && interview.getInterviewer().getId() != null) {
            employeeRepository.findById(interview.getInterviewer().getId())
                .ifPresent(interview::setInterviewer);
        }
        return interviewRepository.save(interview);
    }

    @Transactional
    public HrOffer createOffer(Long orgId, Long applicantId, HrOffer offer, Long createdBy) {
        HrApplicant applicant = applicantRepository.findById(applicantId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Applicant not found"));
        if (!applicant.getOrganizationId().equals(orgId))
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        offer.setOrganizationId(orgId);
        offer.setApplicant(applicant);
        offer.setCreatedBy(createdBy);
        applicant.setStatus("OFFER");
        applicantRepository.save(applicant);
        return offerRepository.save(offer);
    }

    @Transactional
    public HrOffer respondToOffer(Long offerId, String response) {
        HrOffer offer = offerRepository.findById(offerId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Offer not found"));
        offer.setStatus(response);
        offer.setResponseDate(java.time.LocalDate.now());
        if ("ACCEPTED".equals(response)) {
            offer.getApplicant().setStatus("HIRED");
            applicantRepository.save(offer.getApplicant());
        }
        return offerRepository.save(offer);
    }

    public List<HrInterview> getInterviews(Long applicantId) {
        return interviewRepository.findByApplicantId(applicantId);
    }

    public List<HrOffer> getOffers(Long applicantId) {
        return offerRepository.findByApplicantId(applicantId);
    }
}
