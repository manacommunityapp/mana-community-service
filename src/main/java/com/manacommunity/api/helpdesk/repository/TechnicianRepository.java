package com.manacommunity.api.helpdesk.repository;

import com.manacommunity.api.helpdesk.entity.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    List<Technician> findByCommunityIdAndActiveTrue(Long communityId);

    @Query("SELECT t FROM Technician t WHERE t.communityId = :communityId AND t.active = true AND t.isAvailable = true AND (t.primarySkill = :skill OR t.secondarySkills LIKE %:skill%)")
    List<Technician> findAvailableBySkill(@Param("communityId") Long communityId, @Param("skill") String skill);
}
