package com.manacommunity.api.user.repository;

import com.manacommunity.api.user.model.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    List<FamilyMember> findByUserIdOrderByCreatedAtAsc(Long userId);
    List<FamilyMember> findByCommunityIdOrderByCreatedAtAsc(Long communityId);
    List<FamilyMember> findByUserIdAndNameIgnoreCase(Long userId, String name);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
}
