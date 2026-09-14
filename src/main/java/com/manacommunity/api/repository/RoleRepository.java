package com.manacommunity.api.repository;

import com.manacommunity.api.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);

    Optional<Role> findByNameIgnoreCaseAndCommunityId(String name, Long communityId);
    Optional<Role> findByNameIgnoreCaseAndCommunityIdIsNull(String name);
    boolean existsByNameIgnoreCaseAndCommunityId(String name, Long communityId);
    boolean existsByNameIgnoreCaseAndCommunityIdIsNull(String name);

    java.util.List<Role> findByCommunityId(Long communityId);
    java.util.List<Role> findByCommunityIdIsNull();
    java.util.List<Role> findByCommunityIdOrCommunityIdIsNull(Long communityId);
}
