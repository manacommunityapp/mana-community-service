package com.manacommunity.api.support;

import com.manacommunity.api.user.dto.LoginRequest;
import com.manacommunity.api.user.dto.RegisterRequest;
import com.manacommunity.api.model.*;
import com.manacommunity.api.model.scheduler.*;
import com.manacommunity.api.user.model.AppUser;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.manacommunity.api.model.scheduler.TournamentConfig.TournamentStatus;

/**
 * Factory class for building test entities.
 * Use these builders instead of constructing entities inline so that
 * required fields never get missed and changes to entity structure
 * only need to be fixed in one place.
 */
public final class TestDataBuilder {

    private TestDataBuilder() {}

    // ── Community ─────────────────────────────────────────────────────

    public static Community community(Long id, String inviteCode) {
        return Community.builder()
                .id(id)
                .name("Test Community")
                .inviteCode(inviteCode)
                .build();
    }

    public static Community community() {
        return community(1L, "INVITE123");
    }

    // ── Role ─────────────────────────────────────────────────────────

    public static Role role(Long id, String name, Long communityId) {
        return Role.builder()
                .id(id)
                .name(name)
                .communityId(communityId)
                .permissions(new java.util.HashSet<>())
                .build();
    }

    public static Role memberRole() { return role(10L, "MEMBER", 1L); }
    public static Role adminRole()  { return role(11L, "ADMIN",  1L); }

    // ── AppUser ───────────────────────────────────────────────────────

    public static AppUser appUser(Long id, String email, String role) {
        return AppUser.builder()
                .id(id)
                .fullName("Test User")
                .email(email)
                .phone("9" + id + "0000001")
                .passwordHash("$2a$10$hashed")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("MALE")
                .role(role)
                .kycStatus("VERIFIED")
                .isActive(true)
                .community(community())
                .build();
    }

    public static AppUser adminUser()  { return appUser(1L, "admin@test.com", "ADMIN"); }
    public static AppUser memberUser() { return appUser(2L, "member@test.com", "MEMBER"); }
    public static AppUser superAdmin() { return appUser(3L, "super@test.com", "SUPER_ADMIN"); }

    // ── RolePermission ────────────────────────────────────────────────

    public static RolePermission rolePermission(Role role, String permissionKey) {
        return RolePermission.builder()
                .role(role.getName().toUpperCase())
                .roleEntity(role)
                .permissionKey(permissionKey)
                .build();
    }

    // ── Auth DTOs ─────────────────────────────────────────────────────

    public static RegisterRequest registerRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("John Doe");
        req.setEmail("john@example.com");
        req.setPhone("9876543210");
        req.setPassword("Pass1234");
        req.setAadharNumber("123456789012");
        req.setInviteCode("INVITE123");
        req.setDateOfBirth(LocalDate.of(1995, 6, 15));
        req.setGender("MALE");
        req.setBlock("A");
        req.setFlatNo("101");  // Floor 1, Flat 1 of Block A
        req.setUserType("Owner");
        return req;
    }

    public static LoginRequest loginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setIdentifier(email);
        req.setPassword(password);
        return req;
    }

    // ── TournamentConfig ──────────────────────────────────────────────

    public static TournamentConfig tournamentConfig(Long id) {
        return TournamentConfig.builder()
                .id(id)
                .tournamentName("Test Cup")
                .tournamentType(TournamentType.KNOCKOUT)
                .totalTeams(8)
                .startDate(LocalDate.now().plusDays(7))
                .status(TournamentStatus.DRAFT)
                .build();
    }

    // ── TournamentMatch ───────────────────────────────────────────────

    public static TournamentMatch match(TournamentConfig config, int number) {
        return TournamentMatch.builder()
                .config(config)
                .round(MatchRound.FINAL)
                .matchNumber(number)
                .scheduledAt(LocalDateTime.now().plusDays(7))
                .durationMinutes(90)
                .status(MatchStatus.SCHEDULED)
                .build();
    }

    // ── EventCommunity ───────────────────────────────────────────────

    public static com.manacommunity.api.events.entity.EventCommunity communityEvent(Long id, Community community, AppUser user) {
        return com.manacommunity.api.events.entity.EventCommunity.builder()
                .id(id)
                .title("Annual Sports Meet")
                .description("Community annual sports day")
                .type(com.manacommunity.api.events.entity.EventCommunity.EventType.SPORTS)
                .status(com.manacommunity.api.events.entity.EventCommunity.EventStatus.PUBLISHED)
                .startDate(LocalDate.now().plusDays(10))
                .capacity(100)
                .priceType(com.manacommunity.api.events.entity.EventCommunity.PriceType.FREE)
                .community(community != null ? community : community())
                .createdBy(user != null ? user : adminUser())
                .build();
    }

    public static com.manacommunity.api.events.dto.EventRequest eventRequest(String title) {
        com.manacommunity.api.events.dto.EventRequest req = new com.manacommunity.api.events.dto.EventRequest();
        req.setTitle(title);
        req.setDescription("Event description");
        req.setType("SPORTS");
        req.setStartDate(LocalDate.now().plusDays(5).toString());
        req.setCapacity(50);
        req.setPriceType("FREE");
        req.setLocationType("IN_PERSON");
        req.setLocation("Clubhouse");
        return req;
    }
}
