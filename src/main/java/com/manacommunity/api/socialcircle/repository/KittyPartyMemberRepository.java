package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KittyPartyMemberRepository extends JpaRepository<KittyPartyMember, Long> {

    List<KittyPartyMember> findByCircleIdAndMembershipStatus(Long circleId, String membershipStatus);

    Optional<KittyPartyMember> findByCircleIdAndUserId(Long circleId, Long userId);

    boolean existsByCircleIdAndUserIdAndMembershipStatus(Long circleId, Long userId, String membershipStatus);

    long countByCircleIdAndMembershipStatus(Long circleId, String membershipStatus);

    long countByCircleIdAndIsExternalTrueAndMembershipStatus(Long circleId, String membershipStatus);
}
