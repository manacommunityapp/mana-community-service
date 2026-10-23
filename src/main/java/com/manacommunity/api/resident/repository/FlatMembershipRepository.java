package com.manacommunity.api.resident.repository;

import com.manacommunity.api.resident.model.FlatMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlatMembershipRepository extends JpaRepository<FlatMembership, Long> {

    Optional<FlatMembership> findByUserIdAndFlatId(Long userId, Long flatId);

    List<FlatMembership> findByUserId(Long userId);

    @Query("SELECT fm FROM FlatMembership fm JOIN FETCH fm.flat f JOIN FETCH f.community WHERE fm.user.id = :userId")
    List<FlatMembership> findByUserIdWithFlatAndCommunity(@Param("userId") Long userId);

    List<FlatMembership> findByFlatId(Long flatId);

    @Query("SELECT fm FROM FlatMembership fm JOIN FETCH fm.user u WHERE fm.flat.id = :flatId")
    List<FlatMembership> findByFlatIdWithUser(@Param("flatId") Long flatId);

    Optional<FlatMembership> findByFlatIdAndIsPrimaryResidentTrue(Long flatId);
}
