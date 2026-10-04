package com.manacommunity.api.homeservices.service;

import com.manacommunity.api.homeservices.dto.DomesticStaffRequest;
import com.manacommunity.api.homeservices.dto.DomesticStaffResponse;
import com.manacommunity.api.homeservices.model.DomesticStaff;

import java.util.List;

public interface DomesticStaffService {

    List<DomesticStaffResponse> getStaff(Long communityId, DomesticStaff.StaffRole role);

    DomesticStaffResponse getStaffById(Long id);

    DomesticStaffResponse createStaff(Long communityId, DomesticStaffRequest request);
}
