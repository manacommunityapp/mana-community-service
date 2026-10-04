package com.manacommunity.api.homeservices.service.impl;

import com.manacommunity.api.homeservices.dto.DomesticStaffRequest;
import com.manacommunity.api.homeservices.dto.DomesticStaffResponse;
import com.manacommunity.api.homeservices.model.DomesticStaff;
import com.manacommunity.api.homeservices.repository.DomesticStaffRepository;
import com.manacommunity.api.homeservices.service.DomesticStaffService;
import com.manacommunity.api.model.Community;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DomesticStaffServiceImpl implements DomesticStaffService {

    private final DomesticStaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DomesticStaffResponse> getStaff(Long communityId, DomesticStaff.StaffRole role) {
        List<DomesticStaff> staffList;
        if (role != null) {
            staffList = staffRepository.findByCommunityIdAndRoleOrderByNameAsc(communityId, role);
        } else {
            staffList = staffRepository.findByCommunityIdOrderByNameAsc(communityId);
        }
        return staffList.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DomesticStaffResponse getStaffById(Long id) {
        DomesticStaff staff = staffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff not found with id: " + id));
        return toResponse(staff);
    }

    @Override
    @Transactional
    public DomesticStaffResponse createStaff(Long communityId, DomesticStaffRequest request) {
        Community community = new Community();
        community.setId(communityId);

        DomesticStaff staff = DomesticStaff.builder()
                .community(community)
                .name(request.getName())
                .phone(request.getPhone())
                .role(request.getRole())
                .shiftTime(request.getShiftTime())
                .workingTowers(request.getWorkingTowers())
                .monthlySalary(request.getMonthlySalary())
                .verified(request.getVerified() != null ? request.getVerified() : false)
                .policeVerified(request.getPoliceVerified() != null ? request.getPoliceVerified() : false)
                .aadhaarOnFile(request.getAadhaarOnFile() != null ? request.getAadhaarOnFile() : false)
                .experience(request.getExperience())
                .photoUrl(request.getPhotoUrl())
                .build();

        return toResponse(staffRepository.save(staff));
    }

    private DomesticStaffResponse toResponse(DomesticStaff staff) {
        DomesticStaffResponse response = new DomesticStaffResponse();
        response.setId(staff.getId());
        response.setName(staff.getName());
        response.setPhone(staff.getPhone());
        response.setRole(staff.getRole().name());
        response.setShiftTime(staff.getShiftTime());
        response.setWorkingTowers(staff.getWorkingTowers());
        response.setMonthlySalary(staff.getMonthlySalary());
        response.setVerified(staff.getVerified());
        response.setPoliceVerified(staff.getPoliceVerified());
        response.setAadhaarOnFile(staff.getAadhaarOnFile());
        response.setRating(staff.getRating());
        response.setReviewCount(staff.getReviewCount());
        response.setExperience(staff.getExperience());
        response.setStatus(staff.getStatus().name());
        response.setPhotoUrl(staff.getPhotoUrl());
        response.setCreatedAt(staff.getCreatedAt());
        return response;
    }
}
