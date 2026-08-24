package com.manacommunity.api.user.service;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.model.FamilyMember;

import java.util.List;

public interface FamilyMemberService {
    List<FamilyMember> getFamilyMembers(AppUser user, Long communityId);
    FamilyMember addFamilyMember(FamilyMember member, AppUser user, Long communityId);
    FamilyMember updateFamilyMember(Long id, FamilyMember member, AppUser user, Long communityId);
    void deleteFamilyMember(Long id, AppUser user, Long communityId);
}
