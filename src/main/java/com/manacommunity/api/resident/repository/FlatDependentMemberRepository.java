package com.manacommunity.api.resident.repository;

import com.manacommunity.api.resident.model.FlatDependentMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlatDependentMemberRepository extends JpaRepository<FlatDependentMember, Long> {

    List<FlatDependentMember> findByFlatIdAndStatus(Long flatId, String status);

    List<FlatDependentMember> findByFlatId(Long flatId);

    List<FlatDependentMember> findByGuardianUserId(Long guardianUserId);

    @Query("SELECT fdm FROM FlatDependentMember fdm WHERE fdm.flat.id = :flatId AND fdm.status = 'ACTIVE'")
    List<FlatDependentMember> findActiveByFlatId(@Param("flatId") Long flatId);
}
