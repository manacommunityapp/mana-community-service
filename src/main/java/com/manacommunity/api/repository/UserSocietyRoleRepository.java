package com.manacommunity.api.repository;

import com.manacommunity.api.model.UserSocietyRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSocietyRoleRepository extends JpaRepository<UserSocietyRole, Long> {

    List<UserSocietyRole> findByUserIdAndActiveTrue(Long userId);

    List<UserSocietyRole> findByCommunityIdAndActiveTrue(Long communityId);

    Optional<UserSocietyRole> findByUserIdAndCommunityIdAndRoleType(Long userId, Long communityId, String roleType);

    Optional<UserSocietyRole> findByUserIdAndIsPrimaryTrue(Long userId);

    List<UserSocietyRole> findByUserIdAndCommunityId(Long userId, Long communityId);
}
