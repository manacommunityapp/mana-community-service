package com.manacommunity.api.noticeboard.statutory.repository;

import com.manacommunity.api.noticeboard.statutory.entity.AudienceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AudienceMemberRepository extends JpaRepository<AudienceMember, Long> {
    List<AudienceMember> findByAudienceGroupId(Long groupId);

    @Query("SELECT m.towerName, COUNT(m) FROM AudienceMember m WHERE m.audienceGroupId = :groupId GROUP BY m.towerName")
    List<Object[]> countByTowerForGroup(Long groupId);

    long countByAudienceGroupId(Long groupId);
}
