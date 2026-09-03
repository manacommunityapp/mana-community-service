package com.manacommunity.api.user.repository;

import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BUG FIX: Renamed from UserRepository to AppUserRepository to match
 * the AppUser entity (used in SportsEventServiceImpl, AuctionServiceImpl,
 * NotificationScheduler). The old UserRepository worked against the
 * legacy User entity (String PK / "users" table) which conflicts with
 * the schema's app_user table (Long PK).
 *
 * Changed PK type to Long matching AppUser.id (BIGSERIAL).
 * Added findByPhone for duplicate-phone check during registration.
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByEmailIgnoreCase(String email);
    Optional<AppUser> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhone(String phone);
    java.util.List<AppUser> findByCommunityIdAndFullNameContainingIgnoreCase(Long communityId, String query);
    java.util.List<AppUser> findByCommunityId(Long communityId);
    Page<AppUser> findByCommunityId(Long communityId, Pageable pageable);
    java.util.List<AppUser> findByCommunityIdAndIsActiveTrue(Long communityId);
    Page<AppUser> findByKycStatus(String kycStatus, Pageable pageable);
    Page<AppUser> findByCommunityIdAndKycStatus(Long communityId, String kycStatus, Pageable pageable);
    long countByKycStatus(String kycStatus);
    long countByCommunityIdAndKycStatus(Long communityId, String kycStatus);
    long countByCommunityId(Long communityId);
    long countByIsActiveTrue();
    long countByCommunityIdAndIsActiveTrue(Long communityId);

    @Query("SELECT u.role, COUNT(u.id) FROM AppUser u WHERE u.community.id = :communityId GROUP BY u.role")
    List<Object[]> countByRoleGroupedForCommunity(@Param("communityId") Long communityId);

    @Query("SELECT u.role, COUNT(u.id) FROM AppUser u GROUP BY u.role")
    List<Object[]> countByRoleGrouped();
    boolean existsByCommunityIdAndBlockIgnoreCaseAndFlatNoIgnoreCase(Long communityId, String block, String flatNo);

    /**
     * Returns every user in the given community who has the specified role
     * assigned via the {@code app_user_roles} join table.
     * Role comparison is case-insensitive.
     */
    @Query("""
            SELECT DISTINCT u FROM AppUser u
            JOIN u.userRoles r
            WHERE UPPER(r.name) = UPPER(:roleName)
              AND u.community.id = :communityId
            """)
    List<AppUser> findByCommunityIdAndUserRoleName(
            @Param("communityId") Long communityId,
            @Param("roleName")    String roleName);

    /**
     * Returns every user (across all communities) who has the specified role
     * assigned via the {@code app_user_roles} join table (SUPER_ADMIN view).
     */
    @Query("""
            SELECT DISTINCT u FROM AppUser u
            JOIN u.userRoles r
            WHERE UPPER(r.name) = UPPER(:roleName)
            """)
    List<AppUser> findByUserRoleName(@Param("roleName") String roleName);
}
