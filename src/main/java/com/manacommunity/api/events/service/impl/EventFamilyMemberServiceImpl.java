package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.events.entity.EventFamilyMember;
import com.manacommunity.api.events.repository.EventFamilyMemberRepository;
import com.manacommunity.api.events.service.EventFamilyMemberService;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.repository.EventCommunityRepository;

@Service
public class EventFamilyMemberServiceImpl implements EventFamilyMemberService {

    private static final Set<String> DUMMY_NAMES = Set.of(
            "Sunita Sharma", "Aarav Sharma", "Ananya Sharma",
            "Sandeep Verma", "Ananya Verma", "Rahul Verma", "Priya Verma"
    );

    private final EventFamilyMemberRepository repository;
    private final CommunityRepository communityRepository;
    private final EventCommunityRepository eventRepository;

    public EventFamilyMemberServiceImpl(
            EventFamilyMemberRepository repository,
            CommunityRepository communityRepository,
            EventCommunityRepository eventRepository) {
        this.repository = repository;
        this.communityRepository = communityRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional
    public List<EventFamilyMember> getFamilyMembers(AppUser user, Long communityId) {
        return getFamilyMembers(user, communityId, null);
    }

    @Override
    @Transactional
    public List<EventFamilyMember> getFamilyMembers(AppUser user, Long communityId, Long eventId) {
        if (user == null || user.getId() == null) {
            if (eventId != null) {
                return repository.findByEventIdOrderByCreatedAtAsc(eventId);
            }
            if (communityId != null) {
                return repository.findByCommunityIdOrderByCreatedAtAsc(communityId);
            }
            return Collections.emptyList();
        }

        List<EventFamilyMember> list;
        if (eventId != null) {
            list = repository.findByUserIdAndEventIdOrderByCreatedAtAsc(user.getId(), eventId);
            if (list.isEmpty()) {
                list = repository.findByUserIdOrderByCreatedAtAsc(user.getId());
            }
        } else {
            list = repository.findByUserIdOrderByCreatedAtAsc(user.getId());
        }

        List<EventFamilyMember> toDelete = list.stream()
                .filter(m -> m.getName() != null && DUMMY_NAMES.contains(m.getName().trim()))
                .toList();

        if (!toDelete.isEmpty()) {
            repository.deleteAll(toDelete);
            list = (eventId != null)
                    ? repository.findByUserIdAndEventIdOrderByCreatedAtAsc(user.getId(), eventId)
                    : repository.findByUserIdOrderByCreatedAtAsc(user.getId());
        }

        return list;
    }

    @Override
    @Transactional
    public EventFamilyMember addFamilyMember(EventFamilyMember member, AppUser user, Long communityId) {
        return addFamilyMember(member, user, communityId, null);
    }

    @Override
    @Transactional
    public EventFamilyMember addFamilyMember(EventFamilyMember member, AppUser user, Long communityId, Long eventId) {
        Community comm = (user != null && user.getCommunity() != null)
                ? user.getCommunity()
                : (communityId != null ? communityRepository.findById(communityId).orElse(null) : null);

        EventCommunity event = (eventId != null)
                ? eventRepository.findById(eventId).orElse(null)
                : null;

        member.setId(null);
        member.setUser(user);
        member.setCommunity(comm);
        member.setEvent(event);
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
                    .orElseThrow(() -> new IllegalArgumentException("Family member not found: " + id))
                : repository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Family member not found: " + id));

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
                    .orElseThrow(() -> new IllegalArgumentException("Family member not found: " + id))
                : repository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Family member not found: " + id));

        repository.delete(existing);
    }
}
