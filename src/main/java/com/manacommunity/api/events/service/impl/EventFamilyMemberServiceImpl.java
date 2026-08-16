package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.events.entity.EventFamilyMember;
import com.manacommunity.api.events.repository.EventFamilyMemberRepository;
import com.manacommunity.api.events.service.EventFamilyMemberService;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EventFamilyMemberServiceImpl implements EventFamilyMemberService {

    private final EventFamilyMemberRepository repository;
    private final CommunityRepository communityRepository;

    public EventFamilyMemberServiceImpl(EventFamilyMemberRepository repository, CommunityRepository communityRepository) {
        this.repository = repository;
        this.communityRepository = communityRepository;
    }

    @Override
    @Transactional
    public List<EventFamilyMember> getFamilyMembers(AppUser user, Long communityId) {
        if (user == null || user.getId() == null) {
            if (communityId != null) {
                return repository.findByCommunityIdOrderByCreatedAtAsc(communityId);
            }
            return repository.findAll();
        }

        List<EventFamilyMember> existing = repository.findByUserIdOrderByCreatedAtAsc(user.getId());
        if (!existing.isEmpty()) {
            return existing;
        }

        Community comm = user.getCommunity();
        if (comm == null && communityId != null) {
            comm = communityRepository.findById(communityId).orElse(null);
        }

        String primaryName = (user.getFullName() != null && !user.getFullName().isBlank())
                ? user.getFullName()
                : (user.getEmail() != null ? user.getEmail().split("@")[0] : "Devotee (Self)");

        List<EventFamilyMember> seeded = new ArrayList<>();
        seeded.add(EventFamilyMember.builder()
                .user(user)
                .community(comm)
                .name(primaryName)
                .relation("Self")
                .age(34)
                .avatar("👤")
                .gothram("Kashyapa")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        seeded.add(EventFamilyMember.builder()
                .user(user)
                .community(comm)
                .name("Sunita Sharma")
                .relation("Spouse")
                .age(31)
                .avatar("👩")
                .gothram("Kashyapa")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now().plusSeconds(1))
                .updatedAt(LocalDateTime.now().plusSeconds(1))
                .build());

        seeded.add(EventFamilyMember.builder()
                .user(user)
                .community(comm)
                .name("Aarav Sharma")
                .relation("Son")
                .age(8)
                .avatar("👦")
                .gothram("Kashyapa")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now().plusSeconds(2))
                .updatedAt(LocalDateTime.now().plusSeconds(2))
                .build());

        seeded.add(EventFamilyMember.builder()
                .user(user)
                .community(comm)
                .name("Ananya Sharma")
                .relation("Daughter")
                .age(5)
                .avatar("👧")
                .gothram("Kashyapa")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now().plusSeconds(3))
                .updatedAt(LocalDateTime.now().plusSeconds(3))
                .build());

        return repository.saveAll(seeded);
    }

    @Override
    @Transactional
    public EventFamilyMember addFamilyMember(EventFamilyMember member, AppUser user, Long communityId) {
        Community comm = (user != null && user.getCommunity() != null)
                ? user.getCommunity()
                : (communityId != null ? communityRepository.findById(communityId).orElse(null) : null);

        member.setId(null);
        member.setUser(user);
        member.setCommunity(comm);
        if (member.getStatus() == null || member.getStatus().isBlank()) {
            member.setStatus("ACTIVE");
        }
        if (member.getAvatar() == null || member.getAvatar().isBlank()) {
            member.setAvatar("👤");
        }
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        return repository.save(member);
    }

    @Override
    @Transactional
    public EventFamilyMember updateFamilyMember(Long id, EventFamilyMember member, AppUser user, Long communityId) {
        EventFamilyMember existing = (user != null && user.getId() != null)
                ? repository.findByIdAndUserId(id, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Family member", id))
                : repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Family member", id));

        if (member.getName() != null && !member.getName().isBlank()) {
            existing.setName(member.getName());
        }
        if (member.getRelation() != null) existing.setRelation(member.getRelation());
        if (member.getAge() != null) existing.setAge(member.getAge());
        if (member.getAvatar() != null) existing.setAvatar(member.getAvatar());
        if (member.getGender() != null) existing.setGender(member.getGender());
        if (member.getGothram() != null) existing.setGothram(member.getGothram());
        if (member.getNakshatram() != null) existing.setNakshatram(member.getNakshatram());
        if (member.getRasi() != null) existing.setRasi(member.getRasi());
        if (member.getStatus() != null) existing.setStatus(member.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());

        return repository.save(existing);
    }

    @Override
    @Transactional
    public void deleteFamilyMember(Long id, AppUser user, Long communityId) {
        EventFamilyMember existing = (user != null && user.getId() != null)
                ? repository.findByIdAndUserId(id, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Family member", id))
                : repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Family member", id));

        repository.delete(existing);
    }
}
