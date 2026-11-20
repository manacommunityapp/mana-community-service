package com.manacommunity.api.social.repository;

import com.manacommunity.api.social.entity.SocialGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SocialGroupMemberRepository extends JpaRepository<SocialGroupMember, Long> {
    Optional<SocialGroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    List<SocialGroupMember> findByGroupId(Long groupId);

    @Query("SELECT m.groupId FROM SocialGroupMember m WHERE m.userId = :userId")
    List<Long> findGroupIdsByUserId(Long userId);
}
