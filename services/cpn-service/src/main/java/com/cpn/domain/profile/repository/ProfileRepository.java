package com.cpn.domain.profile.repository;

import com.cpn.domain.profile.model.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    @Query("SELECT DISTINCT p FROM Profile p JOIN p.skills s WHERE s.skillName = :skill AND p.tenantId = :tenantId")
    Page<Profile> searchBySkillAndTenantId(@Param("skill") String skill, @Param("tenantId") UUID tenantId, Pageable pageable);

    Page<Profile> findByTenantIdAndIsOpenToWorkTrue(UUID tenantId, Pageable pageable);
}
