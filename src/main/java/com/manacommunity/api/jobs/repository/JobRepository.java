package com.manacommunity.api.jobs.repository;

import com.manacommunity.api.jobs.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<Job> findByPostedByIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT j FROM Job j WHERE j.community.id = :communityId AND j.status = 'ACTIVE' ORDER BY j.createdAt DESC")
    List<Job> findActiveByCommunity(@Param("communityId") Long communityId);

    @Query("SELECT j FROM Job j WHERE j.community.id = :communityId AND j.status = 'ACTIVE' " +
            "AND (LOWER(j.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(j.company) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "ORDER BY j.createdAt DESC")
    List<Job> searchActiveByCommunity(@Param("communityId") Long communityId, @Param("q") String query);
}
