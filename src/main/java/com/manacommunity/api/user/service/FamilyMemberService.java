package com.manacommunity.api.user.service;

import com.manacommunity.api.user.dto.FamilyMemberRequest;
import com.manacommunity.api.user.dto.FamilyMemberResponse;
import com.manacommunity.api.user.dto.FamilyMemberSlimResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.model.FamilyMember;

import java.util.List;

public interface FamilyMemberService {
    List<FamilyMemberResponse> getFamilyMembersResponse(AppUser user, Long communityId);
    List<FamilyMember> getFamilyMembers(AppUser user, Long communityId);
    List<FamilyMemberSlimResponse> getSlimFamilyMembers(Long targetUserId, AppUser currentUser);
    List<FamilyMemberSlimResponse> getSlimFamilyMembers(Long userId);
    FamilyMemberResponse addFamilyMember(FamilyMemberRequest request, AppUser user, Long communityId);
    FamilyMember addFamilyMember(FamilyMember member, AppUser user, Long communityId);
    FamilyMemberResponse updateFamilyMember(Long id, FamilyMemberRequest request, AppUser user, Long communityId);
    FamilyMember updateFamilyMember(Long id, FamilyMember member, AppUser user, Long communityId);
    void deleteFamilyMember(Long id, AppUser user, Long communityId);
}
