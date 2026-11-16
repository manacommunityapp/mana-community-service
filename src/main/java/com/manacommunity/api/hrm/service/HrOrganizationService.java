package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.dto.*;
import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HrOrganizationService {

    private final HrOrganizationRepository organizationRepository;
    private final HrBranchRepository branchRepository;
    private final HrDepartmentRepository departmentRepository;
    private final HrDesignationRepository designationRepository;

    @Transactional
    public OrganizationResponse createOrganization(Long communityId, CreateOrganizationRequest request) {
        HrOrganization org = HrOrganization.builder()
                .communityId(communityId)
                .organizationName(request.getOrganizationName())
                .organizationType(request.getOrganizationType())
                .gstNumber(request.getGstNumber())
                .registrationNumber(request.getRegistrationNumber())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .address(request.getAddress())
                .build();
        org = organizationRepository.save(org);
        return toResponse(org);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(Long id, Long communityId) {
        HrOrganization org = organizationRepository.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        return toResponse(org);
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listOrganizations(Long communityId) {
        return organizationRepository.findByCommunityId(communityId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrganizationResponse updateOrganization(Long id, Long communityId, CreateOrganizationRequest request) {
        HrOrganization org = organizationRepository.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        org.setOrganizationName(request.getOrganizationName());
        org.setOrganizationType(request.getOrganizationType());
        org.setGstNumber(request.getGstNumber());
        org.setRegistrationNumber(request.getRegistrationNumber());
        org.setContactEmail(request.getContactEmail());
        org.setContactPhone(request.getContactPhone());
        org.setAddress(request.getAddress());
        org = organizationRepository.save(org);
        return toResponse(org);
    }

    @Transactional
    public HrBranch addBranch(Long orgId, Long communityId, CreateBranchRequest request) {
        HrOrganization org = organizationRepository.findByIdAndCommunityId(orgId, communityId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        HrBranch branch = HrBranch.builder()
                .organization(org)
                .branchName(request.getBranchName())
                .branchCode(request.getBranchCode())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .build();
        return branchRepository.save(branch);
    }

    @Transactional(readOnly = true)
    public List<HrBranch> listBranches(Long orgId) {
        return branchRepository.findByOrganizationId(orgId);
    }

    @Transactional
    public HrDepartment addDepartment(Long orgId, Long communityId, CreateDepartmentRequest request) {
        HrOrganization org = organizationRepository.findByIdAndCommunityId(orgId, communityId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        HrDepartment dept = HrDepartment.builder()
                .organization(org)
                .departmentName(request.getDepartmentName())
                .departmentCode(request.getDepartmentCode())
                .build();
        return departmentRepository.save(dept);
    }

    @Transactional(readOnly = true)
    public List<HrDepartment> listDepartments(Long orgId) {
        return departmentRepository.findByOrganizationId(orgId);
    }

    @Transactional
    public HrDesignation addDesignation(Long orgId, Long communityId, CreateDesignationRequest request) {
        HrOrganization org = organizationRepository.findByIdAndCommunityId(orgId, communityId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        HrDesignation designation = HrDesignation.builder()
                .organization(org)
                .designationName(request.getDesignationName())
                .level(request.getLevel())
                .build();
        return designationRepository.save(designation);
    }

    @Transactional(readOnly = true)
    public List<HrDesignation> listDesignations(Long orgId) {
        return designationRepository.findByOrganizationId(orgId);
    }

    public HrOrganization findByIdAndCommunityId(Long orgId, Long communityId) {
        return organizationRepository.findByIdAndCommunityId(orgId, communityId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
    }

    private OrganizationResponse toResponse(HrOrganization org) {
        return OrganizationResponse.builder()
                .id(org.getId())
                .communityId(org.getCommunityId())
                .organizationName(org.getOrganizationName())
                .organizationType(org.getOrganizationType())
                .gstNumber(org.getGstNumber())
                .registrationNumber(org.getRegistrationNumber())
                .contactEmail(org.getContactEmail())
                .contactPhone(org.getContactPhone())
                .address(org.getAddress())
                .isActive(org.getIsActive())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }
}
