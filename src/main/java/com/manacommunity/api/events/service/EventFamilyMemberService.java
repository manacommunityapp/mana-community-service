package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.EventFamilyMember;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface EventFamilyMemberService {
    List<EventFamilyMember> getFamilyMembers(AppUser user, Long communityId);
    List<EventFamilyMember> getFamilyMembers(AppUser user, Long communityId, Long eventId);
    EventFamilyMember addFamilyMember(EventFamilyMember member, AppUser user, Long communityId);
    EventFamilyMember addFamilyMember(EventFamilyMember member, AppUser user, Long communityId, Long eventId);
    EventFamilyMember updateFamilyMember(Long id, EventFamilyMember member, AppUser user, Long communityId);
    void deleteFamilyMember(Long id, AppUser user, Long communityId);
}
