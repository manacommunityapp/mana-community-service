package com.manacommunity.api.unit.sports;

import com.manacommunity.api.controller.SampleDataController;
import com.manacommunity.api.service.sample.SampleDataService;
import com.manacommunity.api.service.sample.data.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.sample.data.*;
import com.manacommunity.api.user.repository.AppUserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Sports Seeder Safety Guards & @Profile(!prod) Unit Tests")
class SportsSeederSafetyGuardTest {

    @Mock private EntityManager entityManager;
    @Mock private Environment environment;
    @Mock private RolePermissionSeeder rolePermissionSeeder;
    @Mock private CommunitySeeder communitySeeder;
    @Mock private SportsMetaSeeder sportsMetaSeeder;
    @Mock private UserSeeder userSeeder;
    @Mock private SportsPlayerCategorySeeder playerCategorySeeder;
    @Mock private SportsVenueSeeder venueSeeder;
    @Mock private CommunityLeaderSeeder communityLeaderSeeder;
    @Mock private SportsEventSeeder sportsEventSeeder;
    @Mock private SportsTournamentSeeder tournamentSeeder;
    @Mock private SportsAuctionSeeder auctionSeeder;
    @Mock private InventorySeeder inventorySeeder;
    @Mock private DefaultCommunityModuleDataService defaultCommunityModuleDataService;
    @Mock private RolePermissionDataSeeder rolePermissionDataSeeder;
    @Mock private SportsVenueDataSeeder venueDataSeeder;
    @Mock private SportsCourtDataSeeder courtDataSeeder;
    @Mock private SportsEventRegistrationDataSeeder sportsEventRegistrationDataSeeder;
    @Mock private SportsTournamentLifecycleSeeder tournamentLifecycleSeeder;
    @Mock private EmailTemplateFeeder emailTemplateFeeder;
    @Mock private AppUserRepository userRepo;
    @Mock private SportsEventRegistrationRepository regRepo;

    // SportsEventSeeder mocks
    @Mock private SportsEventRepository sportsEventRepo;
    @Mock private SportsNotificationSchedulerRepository notificationSchedulerRepo;
    @Mock private SportsAuctionConfigRepository auctionConfigRepo;
    @Mock private SportsAuctionConfigCategoryRepository auctionConfigCategoryRepo;
    @Mock private SportsAuctionDisputeCommitteeRepository auctionCommitteeRepo;
    @Mock private SportsAuctionTeamRepository auctionTeamRepo;
    @Mock private SportsAuctionPlayerRepository auctionPlayerRepo;
    @Mock private SportsAuctionBidRepository auctionBidRepo;
    @Mock private SportsAuctionSessionLogRepository auctionSessionLogRepo;
    @Mock private SportsTournamentConfigRepository tournamentConfigRepo;

    // SportsTournamentLifecycleSeeder mocks
    @Mock private SportsTournamentRepository tournamentRepo;
    @Mock private SportsTournamentGroupRepository groupRepo;
    @Mock private SportsTournamentMatchRepository matchRepo;
    @Mock private SportsGroupTeamStandingRepository standingRepo;
    @Mock private SportsMatchResultRepository matchResultRepo;
    @Mock private SportsVenueRepository venueRepo;
    @Mock private SportsCourtRepository courtRepo;

    @Test
    @DisplayName("Verify @Profile(\"!prod\") annotation is present on all seeders and controllers")
    void testProfileAnnotationPresentOnSeedersAndControllers() {
        List<Class<?>> guardedClasses = List.of(
                SampleDataService.class,
                SportsEventSeeder.class,
                SportsTournamentLifecycleSeeder.class,
                SampleDataController.class,
                SportsAuctionSeeder.class
        );

        for (Class<?> clazz : guardedClasses) {
            assertThat(clazz.isAnnotationPresent(Profile.class))
                    .withFailMessage("Class %s must have @Profile annotation", clazz.getSimpleName())
                    .isTrue();
            Profile profile = clazz.getAnnotation(Profile.class);
            assertThat(profile.value())
                    .withFailMessage("Class %s must have @Profile(\"!prod\")", clazz.getSimpleName())
                    .contains("!prod");
        }
    }

    @Test
    @DisplayName("SportsEventSeeder aborts early and does not delete when 'prod' profile is active")
    void testSportsEventSeeder_abortsWhenProdProfileActive() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        SportsEventSeeder seeder = new SportsEventSeeder(
                sportsEventRepo, regRepo, notificationSchedulerRepo, auctionConfigRepo,
                auctionConfigCategoryRepo, auctionCommitteeRepo, auctionTeamRepo,
                auctionPlayerRepo, auctionBidRepo, auctionSessionLogRepo, tournamentConfigRepo,
                environment, communitySeeder, sportsMetaSeeder, venueSeeder, userSeeder, playerCategorySeeder
        );

        seeder.seed();

        verify(regRepo, never()).deleteAll(any());
        verify(sportsEventRepo, never()).delete(any());
    }

    @Test
    @DisplayName("SportsEventSeeder aborts early and never wipes data when user registrations exist")
    void testSportsEventSeeder_abortsWhenRegistrationsExist() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        when(regRepo.count()).thenReturn(15L);

        SportsEventSeeder seeder = new SportsEventSeeder(
                sportsEventRepo, regRepo, notificationSchedulerRepo, auctionConfigRepo,
                auctionConfigCategoryRepo, auctionCommitteeRepo, auctionTeamRepo,
                auctionPlayerRepo, auctionBidRepo, auctionSessionLogRepo, tournamentConfigRepo,
                environment, communitySeeder, sportsMetaSeeder, venueSeeder, userSeeder, playerCategorySeeder
        );

        seeder.seed();

        verify(regRepo, never()).deleteAll(any());
        verify(sportsEventRepo, never()).delete(any());
    }

    @Test
    @DisplayName("SportsTournamentLifecycleSeeder aborts early when 'prod' profile is active")
    void testSportsTournamentLifecycleSeeder_abortsWhenProdProfileActive() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        SportsTournamentLifecycleSeeder seeder = new SportsTournamentLifecycleSeeder(
                tournamentRepo, sportsEventRepo, regRepo, tournamentConfigRepo, groupRepo,
                matchRepo, standingRepo, matchResultRepo, auctionTeamRepo, auctionConfigRepo,
                venueRepo, courtRepo, sportsMetaSeeder, venueSeeder, communitySeeder, userSeeder, environment
        );

        seeder.seed();

        verify(tournamentRepo, never()).save(any());
        verify(tournamentRepo, never()).delete(any());
    }

    @Test
    @DisplayName("SportsTournamentLifecycleSeeder aborts early when user registrations exist")
    void testSportsTournamentLifecycleSeeder_abortsWhenRegistrationsExist() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        when(regRepo.count()).thenReturn(7L);

        SportsTournamentLifecycleSeeder seeder = new SportsTournamentLifecycleSeeder(
                tournamentRepo, sportsEventRepo, regRepo, tournamentConfigRepo, groupRepo,
                matchRepo, standingRepo, matchResultRepo, auctionTeamRepo, auctionConfigRepo,
                venueRepo, courtRepo, sportsMetaSeeder, venueSeeder, communitySeeder, userSeeder, environment
        );

        seeder.seed();

        verify(tournamentRepo, never()).save(any());
        verify(tournamentRepo, never()).delete(any());
    }

    @Test
    @DisplayName("SampleDataService.run() aborts early when 'prod' profile is active")
    void testSampleDataService_run_abortsWhenProdProfileActive() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        SampleDataService service = createSampleDataService();
        service.run(mock(ApplicationArguments.class));

        verify(userRepo, never()).count();
        verify(sportsEventSeeder, never()).seed();
    }

    @Test
    @DisplayName("SampleDataService.run() aborts early when user registrations exist")
    void testSampleDataService_run_abortsWhenRegistrationsExist() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        when(regRepo.count()).thenReturn(3L);

        SampleDataService service = createSampleDataService();
        service.run(mock(ApplicationArguments.class));

        verify(userRepo, never()).count();
        verify(sportsEventSeeder, never()).seed();
    }

    @Test
    @DisplayName("SampleDataService endpoints block operations in 'prod' profile")
    void testSampleDataService_methods_blockInProdProfile() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        SampleDataService service = createSampleDataService();

        String resultSql = service.executeSampleDataSql();
        assertThat(resultSql).contains("Sample data seeding blocked in 'prod' profile.");

        Map<String, Object> feedResult = service.feedSportsData(false);
        assertThat(feedResult.get("status")).isEqualTo("BLOCKED");

        service.seedSportsLifecycleOnly();
        verify(tournamentLifecycleSeeder, never()).seed();
    }

    @Test
    @DisplayName("SampleDataService endpoints block operations when user registrations exist")
    void testSampleDataService_methods_blockWhenRegistrationsExist() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(regRepo.count()).thenReturn(25L);

        SampleDataService service = createSampleDataService();

        String resultSql = service.executeSampleDataSql();
        assertThat(resultSql).contains("25 user registrations exist in the database");

        Map<String, Object> feedResult = service.feedSportsData(false);
        assertThat(feedResult.get("status")).isEqualTo("BLOCKED");
        assertThat((String) feedResult.get("message")).contains("25 user registrations exist in database");

        service.seedSportsLifecycleOnly();
        verify(tournamentLifecycleSeeder, never()).seed();
    }

    private SampleDataService createSampleDataService() {
        return new SampleDataService(
                entityManager, environment, rolePermissionSeeder, communitySeeder,
                sportsMetaSeeder, userSeeder, playerCategorySeeder, venueSeeder,
                communityLeaderSeeder, sportsEventSeeder, tournamentSeeder, auctionSeeder,
                inventorySeeder, defaultCommunityModuleDataService, rolePermissionDataSeeder,
                venueDataSeeder, courtDataSeeder, sportsEventRegistrationDataSeeder,
                tournamentLifecycleSeeder, emailTemplateFeeder, userRepo, regRepo
        );
    }
}
