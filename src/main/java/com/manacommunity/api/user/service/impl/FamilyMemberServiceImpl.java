package com.manacommunity.api.user.service.impl;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.model.FamilyMember;
import com.manacommunity.api.user.repository.FamilyMemberRepository;
import com.manacommunity.api.user.service.FamilyMemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class FamilyMemberServiceImpl implements FamilyMemberService {

    private final FamilyMemberRepository repository;
    private final CommunityRepository communityRepository;

    public FamilyMemberServiceImpl(FamilyMemberRepository repository, CommunityRepository communityRepository) {
        this.repository = repository;
        this.communityRepository = communityRepository;
    }

    @Override
    @Transactional
    public List<FamilyMember> getFamilyMembers(AppUser user, Long communityId) {
        if (user == null || user.getId() == null) {
            if (communityId != null) {
                return repository.findByCommunityIdOrderByCreatedAtAsc(communityId);
            }
            return Collections.emptyList();
        }
        return repository.findByUserIdOrderByCreatedAtAsc(user.getId());
    }

    @Override
    @Transactional
    public FamilyMember addFamilyMember(FamilyMember member, AppUser user, Long communityId) {
        Community comm = (user != null && user.getCommunity() != null)
                ? user.getCommunity()
                : (communityId != null ? communityRepository.findById(communityId).orElse(null) : null);

        member.setId(null);
        member.setUser(user);
        member.setCommunity(comm);
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());

        if (member.getAvatar() == null || member.getAvatar().isBlank()) {
            boolean isFemale = "Female".equalsIgnoreCase(member.getGender());
            int age = member.getAge() != null ? member.getAge() : 30;
            if (isFemale) {
                member.setAvatar(age < 18 ? "👧" : "👩");
            } else {
                member.setAvatar(age < 18 ? "👦" : "👨");
            }
        }

        return repository.save(member);
    }

    @Override
    @Transactional
    public FamilyMember updateFamilyMember(Long id, FamilyMember member, AppUser user, Long communityId) {
        FamilyMember existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Family member not found with id: " + id));

        if (member.getName() != null && !member.getName().isBlank()) {
            existing.setName(member.getName().trim());
        }
        if (member.getRelation() != null) {
            existing.setRelation(member.getRelation());
        }
        if (member.getAge() != null) {
            existing.setAge(member.getAge());
        }
        if (member.getGender() != null) {
            existing.setGender(member.getGender());
        }
        if (member.getDob() != null) {
            existing.setDob(member.getDob());
        }
        if (member.getPhone() != null) {
            existing.setPhone(member.getPhone());
        }
        if (member.getEmail() != null) {
            existing.setEmail(member.getEmail());
        }
        if (member.getBloodGroup() != null) {
            existing.setBloodGroup(member.getBloodGroup());
        }
        if (member.getGothram() != null) {
            existing.setGothram(member.getGothram());
        }
        if (member.getEmergencyContact() != null) {
            existing.setEmergencyContact(member.getEmergencyContact());
        }
        if (member.getIsDevotee() != null) {
            existing.setIsDevotee(member.getIsDevotee());
        }
        if (member.getAvatar() != null) {
            existing.setAvatar(member.getAvatar());
        }
        if (member.getNotes() != null) {
            existing.setNotes(member.getNotes());
        }
        if (member.getStatus() != null) {
            existing.setStatus(member.getStatus());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        return repository.save(existing);
    }

    @Override
    @Transactional
    public void deleteFamilyMember(Long id, AppUser user, Long communityId) {
        repository.deleteById(id);
    }
}
