package com.manacommunity.api.repository;

import com.manacommunity.api.model.SportsEventRegistration;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsEventRegistrationRepository extends JpaRepository<SportsEventRegistration, Long> {
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
    boolean existsByEventIdAndUserIdAndPlayerName(Long eventId, Long userId, String playerName);
    boolean existsByEventIdAndUserIdAndStatusIn(Long eventId, Long userId, List<SportsEventRegistration.RegistrationStatus> statuses);
    boolean existsByEventIdAndFamilyMemberIdAndStatusIn(Long eventId, Long familyMemberId, List<SportsEventRegistration.RegistrationStatus> statuses);
    boolean existsByEventIdAndPartnerIdAndStatusIn(Long eventId, Long partnerId, List<SportsEventRegistration.RegistrationStatus> statuses);
    boolean existsByEventIdAndPartnerFamilyMemberIdAndStatusIn(Long eventId, Long partnerFamilyMemberId, List<SportsEventRegistration.RegistrationStatus> statuses);
    boolean existsByUserIdAndStatusIn(Long userId, List<SportsEventRegistration.RegistrationStatus> statuses);
    boolean existsByFamilyMemberIdAndStatusIn(Long familyMemberId, List<SportsEventRegistration.RegistrationStatus> statuses);
    boolean existsByPartnerIdAndStatusIn(Long partnerId, List<SportsEventRegistration.RegistrationStatus> statuses);
    boolean existsByPartnerFamilyMemberIdAndStatusIn(Long partnerFamilyMemberId, List<SportsEventRegistration.RegistrationStatus> statuses);

    boolean existsByEventIdAndUserIsNullAndPlayerName(Long eventId, String playerName);
    boolean existsByUserIdAndAgeGreaterThanEqual(Long userId, Integer age);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(r) > 0 FROM SportsEventRegistration r
        WHERE r.event.id = :eventId
          AND r.status NOT IN (com.manacommunity.api.model.SportsEventRegistration$RegistrationStatus.WITHDRAWN,
                               com.manacommunity.api.model.SportsEventRegistration$RegistrationStatus.REJECTED)
          AND LOWER(r.playerName) = LOWER(:playerName)
          AND LOWER(r.email) = LOWER(:email)
          AND LOWER(r.flatNumber) = LOWER(:flatNumber)
    """)
    boolean existsDuplicateRegistration(
            @org.springframework.data.repository.query.Param("eventId") Long eventId,
            @org.springframework.data.repository.query.Param("playerName") String playerName,
            @org.springframework.data.repository.query.Param("email") String email,
            @org.springframework.data.repository.query.Param("flatNumber") String flatNumber);
    long countByEventId(Long eventId);

    @EntityGraph(attributePaths = {"event", "event.sport", "user", "category", "partner", "familyMember", "partnerFamilyMember"})
    List<SportsEventRegistration> findByEventId(Long eventId);

    @EntityGraph(attributePaths = {"event", "event.sport", "user", "category", "partner", "familyMember", "partnerFamilyMember"})
    List<SportsEventRegistration> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"event", "event.sport", "user", "category", "partner", "familyMember", "partnerFamilyMember"})
    List<SportsEventRegistration> findByPartnerId(Long partnerId);

    @EntityGraph(attributePaths = {"event", "event.sport", "user", "category", "partner", "familyMember", "partnerFamilyMember"})
    List<SportsEventRegistration> findByPartnerIdAndPartnerConfirmationStatus(
            Long partnerId, SportsEventRegistration.PartnerConfirmationStatus partnerConfirmationStatus);

    List<SportsEventRegistration> findByEventIdAndStatus(Long eventId, SportsEventRegistration.RegistrationStatus status);
    long countByEventIdAndStatus(Long eventId, SportsEventRegistration.RegistrationStatus status);

//    @org.springframework.data.jpa.repository.Query("""
//        SELECT COUNT(r) FROM SportsEventRegistration r
//        WHERE r.event.community.id = :communityId
//          AND r.event.sport.id = :sportId
//          AND r.status = :registrationStatus
//          AND r.event.registrationStatus NOT IN :excludedStatuses
//    """)
    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(r) FROM SportsEventRegistration r
        WHERE r.event.community.id = :communityId
          AND r.event.sport.id = :sportId
          AND r.status NOT IN (com.manacommunity.api.model.SportsEventRegistration$RegistrationStatus.WITHDRAWN,
                               com.manacommunity.api.model.SportsEventRegistration$RegistrationStatus.REJECTED)
    """)
    long countActiveRegistrationsForCommunityAndSport(
            @org.springframework.data.repository.query.Param("communityId") Long communityId,
            @org.springframework.data.repository.query.Param("sportId") Long sportId);
    @org.springframework.data.jpa.repository.Query("SELECT r FROM SportsEventRegistration r WHERE r.event.community.id = :communityId")
    List<SportsEventRegistration> findByCommunityId(@org.springframework.data.repository.query.Param("communityId") Long communityId);
}
