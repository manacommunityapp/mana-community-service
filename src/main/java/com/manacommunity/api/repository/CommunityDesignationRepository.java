package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommunityDesignation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityDesignationRepository extends JpaRepository<CommunityDesignation, Long> {

    @Query("""
            SELECT d FROM CommunityDesignation d
            WHERE d.communityId = :communityId OR d.communityId IS NULL
            ORDER BY d.displayOrder ASC, d.name ASC
            """)
    List<CommunityDesignation> findAvailableDesignations(@Param("communityId") Long communityId);

    boolean existsByNameIgnoreCaseAndCommunityId(String name, Long communityId);

    boolean existsByNameIgnoreCaseAndCommunityIdIsNull(String name);
}
