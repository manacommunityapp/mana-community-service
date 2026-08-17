package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventFamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventFamilyMemberRepository extends JpaRepository<EventFamilyMember, Long> {
    List<EventFamilyMember> findByUserIdOrderByCreatedAtAsc(Long userId);
    List<EventFamilyMember> findByUserIdAndEventIdOrderByCreatedAtAsc(Long userId, Long eventId);
    List<EventFamilyMember> findByCommunityIdOrderByCreatedAtAsc(Long communityId);
    List<EventFamilyMember> findByEventIdOrderByCreatedAtAsc(Long eventId);
    Optional<EventFamilyMember> findByIdAndUserId(Long id, Long userId);
}
