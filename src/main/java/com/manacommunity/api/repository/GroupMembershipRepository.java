package com.manacommunity.api.repository;

import com.manacommunity.api.model.GroupMembership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {

    Optional<GroupMembership> findByGroupIdAndUserId(Long groupId, Long userId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    Page<GroupMembership> findByGroupIdAndStatus(Long groupId, String status, Pageable pageable);

    List<GroupMembership> findByUserIdAndStatus(Long userId, String status);

    void deleteByGroupIdAndUserId(Long groupId, Long userId);

    long countByGroupIdAndStatus(Long groupId, String status);

    @Query("SELECT COUNT(gm) FROM GroupMembership gm WHERE gm.user.id = :userId AND gm.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT COUNT(gm) FROM GroupMembership gm WHERE gm.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT gm.group.id FROM GroupMembership gm WHERE gm.user.id = :userId AND gm.status = 'ACTIVE'")
    List<Long> findActiveGroupIdsByUserId(@Param("userId") Long userId);
}
