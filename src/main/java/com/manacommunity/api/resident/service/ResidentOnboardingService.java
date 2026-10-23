package com.manacommunity.api.resident.service;

import com.manacommunity.api.resident.dto.*;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface ResidentOnboardingService {

    ResidentOnboardingResponse onboardResident(ResidentOnboardingRequest request, AppUser currentUser);

    FamilyRosterResponse getFamilyRoster(Long flatId, AppUser currentUser);

    FamilyRosterResponse addAdultMember(AddAdultMemberRequest request, AppUser currentUser);

    FamilyRosterResponse addChildDependent(AddChildMemberRequest request, AppUser currentUser);

    List<UserPropertySummaryDto> getUserProperties(AppUser currentUser);

    void removeDependent(Long dependentId, AppUser currentUser);

    void removeAdultMember(Long membershipId, AppUser currentUser);
}
