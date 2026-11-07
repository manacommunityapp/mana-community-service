package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KittyPartyInvitationRepository extends JpaRepository<KittyPartyInvitation, Long> {

    Optional<KittyPartyInvitation> findByInviteTokenHash(String tokenHash);

    List<KittyPartyInvitation> findByCircleIdAndStatus(Long circleId, String status);

    List<KittyPartyInvitation> findByInvitedUserIdAndStatus(Long userId, String status);
}
