package com.manacommunity.api.repository;

import com.manacommunity.api.model.Community;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * If this repository already exists in the project, add the
 * findByInviteCode method to the existing interface.
 */
@Repository
public interface CommunityRepository extends JpaRepository<Community, Long> {
    Optional<Community> findByInviteCode(String inviteCode);
    boolean existsByInviteCode(String inviteCode);
}
