package com.manacommunity.api.childsecurity.repository;

import com.manacommunity.api.childsecurity.entity.GuardianAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuardianAuthorizationRepository extends JpaRepository<GuardianAuthorization, Long> {
    List<GuardianAuthorization> findByChildIdAndIsActiveTrue(Long childId);

    @Query("SELECT g FROM GuardianAuthorization g WHERE g.childId = :childId AND g.isActive = true ORDER BY g.escalationOrder")
    List<GuardianAuthorization> findByChildIdOrderByEscalation(Long childId);

    List<GuardianAuthorization> findByChildIdAndRole(Long childId, GuardianAuthorization.GuardianRole role);
}
