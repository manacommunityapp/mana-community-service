package com.manacommunity.api.user.repository;

import com.manacommunity.api.user.model.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    List<FamilyMember> findByUserIdOrderByCreatedAtAsc(Long userId);
    List<FamilyMember> findByCommunityIdOrderByCreatedAtAsc(Long communityId);
    List<FamilyMember> findByUserIdAndNameIgnoreCase(Long userId, String name);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
    Optional<FamilyMember> findByIdAndUserId(Long id, Long userId);

    /**
     * Full-text search across family members in a community by name.
     * Used by the partner-search endpoint to surface family members
     * alongside flat owners/tenants in the doubles partner selector.
     */
    @Query("""
            SELECT fm FROM FamilyMember fm
            WHERE fm.community.id = :communityId
              AND fm.status = 'ACTIVE'
              AND LOWER(fm.name) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY fm.name ASC
            """)
    List<FamilyMember> searchByNameInCommunity(
            @Param("communityId") Long communityId,
            @Param("query") String query);
}
