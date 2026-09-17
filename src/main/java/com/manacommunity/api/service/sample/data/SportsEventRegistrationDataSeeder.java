package com.manacommunity.api.service.sample.data;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.SportsPlayerCategory;
import com.manacommunity.api.model.SportsEvent;
import com.manacommunity.api.model.SportsEventRegistration;
import com.manacommunity.api.repository.SportsEventRegistrationRepository;
import com.manacommunity.api.repository.SportsEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SportsEventRegistrationDataSeeder — dedicated seeder for the
 * {@code sports_event_registration} table. Registers players into
 * events (idempotent per event/user/playerName) so it coexists
 * with {@link SportsEventSeeder}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsEventRegistrationDataSeeder {

    private final SportsEventRegistrationRepository regRepo;
    private final SportsEventRepository sportsEventRepo;
    private final SportsEventDataSeeder sportsEventDataSeeder;
    private final UserSeeder userSeeder;
    private final SportsPlayerCategorySeeder playerCategorySeeder;

    @Transactional
    public void seed() {
        log.info("SportsEventRegistrationDataSeeder: seed called");
    }

    /**
     * Seeds sample registrations across all sports sub-events.
     */
    @Transactional
    public int seedAllSportsRegistrations() {
        log.info("Seeding sample participant registrations across all sports sub-events...");
        int total = 0;
        total += seedBadmintonRegistrations();
        total += seedTableTennisRegistrations();
        total += seedChessRegistrations();
        total += seedCarromsRegistrations();
        total += seedVolleyballRegistrations();
        log.info("✓ Total sample registrations seeded across all sports: {}", total);
        return total;
    }

    /**
     * Seeds 16 badminton participant registrations for "Badminton - Singles & Doubles".
     */
    public int seedBadmintonRegistrations() {
        SportsEvent badmintonEvent = sportsEventRepo.findAll().stream()
                .filter(e -> e.getName() != null && e.getName().equalsIgnoreCase("Badminton - Singles & Doubles"))
                .findFirst()
                .orElse(null);

        if (badmintonEvent == null) {
            log.warn("⚠ 'Badminton - Singles & Doubles' event not found. Skipping badminton registrations.");
            return 0;
        }

        SportsPlayerCategory badMen = playerCategorySeeder.getCategoryByName("Badminton Men (19+)");
        SportsPlayerCategory badWomen = playerCategorySeeder.getCategoryByName("Badminton Women (19+)");
        SportsPlayerCategory badKids = playerCategorySeeder.getCategoryByName("Badminton (Under-12)");

        int created = 0;
        // Men 19+ (8 players)
        created += register(badmintonEvent, userSeeder.getSandeep(), badMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Sandeep Kamarapu", 35, null, "B-402");
        created += register(badmintonEvent, userSeeder.getSunil(), badMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Sunil Kanthala", 30, null, "C-101");
        created += register(badmintonEvent, userSeeder.getRamesh(), badMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Ramesh Korlakunta", 26, null, "C-102");
        created += register(badmintonEvent, userSeeder.getMady(), badMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.REGISTERED, "Mady", 28, null, "A-103");
        created += register(badmintonEvent, userSeeder.getUserByEmail("rahul.sharma@gmail.com"), badMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Rahul Sharma", 28, null, "D-105");
        created += register(badmintonEvent, userSeeder.getUserByEmail("amit.kumar@gmail.com"), badMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Amit Kumar", 40, null, "C-107");
        created += register(badmintonEvent, userSeeder.getUserByEmail("vikram.singh@gmail.com"), badMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Vikram Singh", 28, null, "C-109");
        created += register(badmintonEvent, userSeeder.getUserByEmail("rohit.verma@gmail.com"), badMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Rohit Verma", 36, null, "D-111");

        // Women 19+ (5 players)
        created += register(badmintonEvent, userSeeder.getUserByEmail("priya.patel@gmail.com"), badWomen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Priya Patel", 28, null, "C-106");
        created += register(badmintonEvent, userSeeder.getUserByEmail("sneha.reddy@gmail.com"), badWomen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Sneha Reddy", 28, null, "A-108");
        created += register(badmintonEvent, userSeeder.getUserByEmail("ananya.desai@gmail.com"), badWomen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Ananya Desai", 28, null, "C-110");
        created += register(badmintonEvent, userSeeder.getUserByEmail("neha.gupta@gmail.com"), badWomen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Neha Gupta", 28, null, "C-112");
        created += register(badmintonEvent, userSeeder.getUserByEmail("pooja.joshi@gmail.com"), badWomen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Pooja Joshi", 28, null, "C-114");

        // Under-12 (3 players)
        created += register(badmintonEvent, userSeeder.getSandeep(), badKids, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Aarav Sharma", 10, null, "B-402");
        created += register(badmintonEvent, userSeeder.getSunil(), badKids, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Ananya Nair", 9, null, "C-101");
        created += register(badmintonEvent, userSeeder.getRamesh(), badKids, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Vihaan Kumar", 11, null, "C-102");

        log.info("✓ Badminton registrations seeded: {} players for {}", created, badmintonEvent.getName());
        return created;
    }

    /**
     * Seeds 8 table tennis participant registrations.
     */
    public int seedTableTennisRegistrations() {
        SportsEvent ttEvent = sportsEventRepo.findAll().stream()
                .filter(e -> e.getName() != null && e.getName().equalsIgnoreCase("Table Tennis - Singles and Doubles"))
                .findFirst()
                .orElse(null);

        if (ttEvent == null) return 0;

        SportsPlayerCategory ttMen = playerCategorySeeder.getCategoryByName("TT Men (19+)");
        SportsPlayerCategory ttWomen = playerCategorySeeder.getCategoryByName("TT Women (19+)");
        SportsPlayerCategory ttBoys = playerCategorySeeder.getCategoryByName("TT Boys (12-19)");

        int created = 0;
        created += register(ttEvent, userSeeder.getSandeep(), ttMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Sandeep Kamarapu", 35, null, "B-402");
        created += register(ttEvent, userSeeder.getSunil(), ttMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Sunil Kanthala", 30, null, "C-101");
        created += register(ttEvent, userSeeder.getUserByEmail("karan.malhotra@gmail.com"), ttMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Karan Malhotra", 28, null, "B-113");
        created += register(ttEvent, userSeeder.getUserByEmail("suresh.nair@gmail.com"), ttMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Suresh Nair", 32, null, "B-115");
        created += register(ttEvent, userSeeder.getUserByEmail("sneha.reddy@gmail.com"), ttWomen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Sneha Reddy", 28, null, "A-108");
        created += register(ttEvent, userSeeder.getUserByEmail("pooja.joshi@gmail.com"), ttWomen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Pooja Joshi", 28, null, "C-114");
        created += register(ttEvent, userSeeder.getUserByEmail("rahul.sharma@gmail.com"), ttBoys, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Rohan Sharma", 15, null, "D-105");
        created += register(ttEvent, userSeeder.getUserByEmail("amit.kumar@gmail.com"), ttBoys, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Dhruv Kumar", 16, null, "C-107");

        log.info("✓ Table Tennis registrations seeded: {} players for {}", created, ttEvent.getName());
        return created;
    }

    /**
     * Seeds 8 chess participant registrations.
     */
    public int seedChessRegistrations() {
        SportsEvent chessEvent = sportsEventRepo.findAll().stream()
                .filter(e -> e.getName() != null && e.getName().equalsIgnoreCase("Chess Championship"))
                .findFirst()
                .orElse(null);

        if (chessEvent == null) return 0;

        SportsPlayerCategory chessMen = playerCategorySeeder.getCategoryByName("Chess Men (19+)");
        SportsPlayerCategory chessWomen = playerCategorySeeder.getCategoryByName("Chess Women (19+)");
        SportsPlayerCategory chessKids = playerCategorySeeder.getCategoryByName("Chess (Under-12)");

        int created = 0;
        created += register(chessEvent, userSeeder.getRamesh(), chessMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Ramesh Korlakunta", 36, null, "B-907");
        created += register(chessEvent, userSeeder.getUserByEmail("siddharth.bose@gmail.com"), chessMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Siddharth Bose", 33, null, "C-301");
        created += register(chessEvent, userSeeder.getUserByEmail("varun.mehta@gmail.com"), chessMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Varun Mehta", 38, null, "C-302");
        created += register(chessEvent, userSeeder.getUserByEmail("gourav.pandey@gmail.com"), chessMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Gourav Pandey", 31, null, "C-303");
        created += register(chessEvent, userSeeder.getUserByEmail("priya.patel@gmail.com"), chessWomen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Priya Patel", 28, null, "A-102");
        created += register(chessEvent, userSeeder.getUserByEmail("ananya.desai@gmail.com"), chessWomen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Ananya Desai", 28, null, "A-202");
        created += register(chessEvent, userSeeder.getSandeep(), chessKids, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Aditya Sharma", 10, null, "B-806");
        created += register(chessEvent, userSeeder.getSunil(), chessKids, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Riya Kanthala", 11, null, "C-212");

        log.info("✓ Chess registrations seeded: {} players for {}", created, chessEvent.getName());
        return created;
    }

    /**
     * Seeds 8 carroms participant registrations.
     */
    public int seedCarromsRegistrations() {
        SportsEvent carromEvent = sportsEventRepo.findAll().stream()
                .filter(e -> e.getName() != null && e.getName().equalsIgnoreCase("Carroms - Singles and Doubles"))
                .findFirst()
                .orElse(null);

        if (carromEvent == null) return 0;

        SportsPlayerCategory carromMen = playerCategorySeeder.getCategoryByName("Carroms Men (19+)");
        SportsPlayerCategory carromWomen = playerCategorySeeder.getCategoryByName("Carroms Women (19+)");
        SportsPlayerCategory carromBoys = playerCategorySeeder.getCategoryByName("Carroms Boys (12-19)");

        int created = 0;
        created += register(carromEvent, userSeeder.getSunil(), carromMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Sunil Kanthala", 36, null, "C-212");
        created += register(carromEvent, userSeeder.getUserByEmail("Bhupal@gmail.com"), carromMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Bhupal", 38, null, "B-209");
        created += register(carromEvent, userSeeder.getUserByEmail("prashant.kadam@gmail.com"), carromMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Prashant Kadam", 49, null, "C-306");
        created += register(carromEvent, userSeeder.getUserByEmail("harsh.vardhan@gmail.com"), carromMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Harsh Vardhan", 35, null, "D-101");
        created += register(carromEvent, userSeeder.getUserByEmail("sneha.reddy@gmail.com"), carromWomen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Sneha Reddy", 28, null, "A-104");
        created += register(carromEvent, userSeeder.getUserByEmail("neha.gupta@gmail.com"), carromWomen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Neha Gupta", 28, null, "A-204");
        created += register(carromEvent, userSeeder.getUserByEmail("vikram.singh@gmail.com"), carromBoys, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Aryan Singh", 14, null, "A-201");
        created += register(carromEvent, userSeeder.getUserByEmail("yash.chopra@gmail.com"), carromBoys, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Yash Chopra Jr", 17, null, "D-102");

        log.info("✓ Carroms registrations seeded: {} players for {}", created, carromEvent.getName());
        return created;
    }

    /**
     * Seeds 12 volleyball participant registrations.
     */
    public int seedVolleyballRegistrations() {
        SportsEvent vEvent = sportsEventRepo.findAll().stream()
                .filter(e -> e.getName() != null && e.getName().equalsIgnoreCase("Volleyball Premier League"))
                .findFirst()
                .orElse(null);

        if (vEvent == null) return 0;

        SportsPlayerCategory vMen = playerCategorySeeder.getCategoryByName("Volleyball Men (15+)");

        int created = 0;
        created += register(vEvent, userSeeder.getSandeep(), vMen, SportsEvent.MatchFormat.TEAM, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Sandeep Kamarapu", 36, "Spiker", "B-806");
        created += register(vEvent, userSeeder.getSunil(), vMen, SportsEvent.MatchFormat.TEAM, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Sunil Kanthala", 36, "Setter", "C-212");
        created += register(vEvent, userSeeder.getRamesh(), vMen, SportsEvent.MatchFormat.TEAM, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Ramesh Korlakunta", 36, "Blocker", "B-907");
        created += register(vEvent, userSeeder.getMady(), vMen, SportsEvent.MatchFormat.TEAM, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Mady", 30, "Libero", "D-107");
        created += register(vEvent, userSeeder.getUserByEmail("rahul.sharma@gmail.com"), vMen, SportsEvent.MatchFormat.TEAM, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Rahul Sharma", 38, "Spiker", "A-101");
        created += register(vEvent, userSeeder.getUserByEmail("amit.kumar@gmail.com"), vMen, SportsEvent.MatchFormat.TEAM, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Amit Kumar", 41, "Server", "A-103");
        created += register(vEvent, userSeeder.getUserByEmail("vikram.singh@gmail.com"), vMen, SportsEvent.MatchFormat.TEAM, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Vikram Singh", 36, "Setter", "A-201");
        created += register(vEvent, userSeeder.getUserByEmail("rohit.verma@gmail.com"), vMen, SportsEvent.MatchFormat.TEAM, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Rohit Verma", 44, "Blocker", "A-203");
        created += register(vEvent, userSeeder.getUserByEmail("karan.malhotra@gmail.com"), vMen, SportsEvent.MatchFormat.TEAM, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Karan Malhotra", 32, "Spiker", "A-301");
        created += register(vEvent, userSeeder.getUserByEmail("suresh.nair@gmail.com"), vMen, SportsEvent.MatchFormat.TEAM, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Suresh Nair", 48, "Libero", "A-303");
        created += register(vEvent, userSeeder.getUserByEmail("Bhupal@gmail.com"), vMen, SportsEvent.MatchFormat.TEAM, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Bhupal", 38, "Universal", "B-209");
        created += register(vEvent, userSeeder.getUserByEmail("naveen.kumar@gmail.com"), vMen, SportsEvent.MatchFormat.TEAM, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Naveen Kumar", 41, "Blocker", "D-104");

        log.info("✓ Volleyball registrations seeded: {} players for {}", created, vEvent.getName());
        return created;
    }

    /**
     * Idempotent registration helper. Checks existence by event + user + playerName
     * before inserting.
     */
    private int register(SportsEvent event, AppUser user, SportsPlayerCategory category,
                         SportsEvent.MatchFormat matchType,
                         SportsEventRegistration.RegistrationStatus status,
                         String playerName, int age, String role,
                         String flatNumberOverride) {
        if (user == null || event == null || category == null) return 0;
        if (regRepo.existsByEventIdAndUserIdAndPlayerName(event.getId(), user.getId(), playerName)) {
            return 0; // already exists
        }
        String flatNumber = flatNumberOverride != null
                ? flatNumberOverride
                : (user.getBlock() + " " + user.getFlatNo());

        regRepo.save(SportsEventRegistration.builder()
                .event(event)
                .user(user)
                .category(category)
                .matchType(matchType)
                .status(status)
                .playerName(playerName)
                .age(age)
                .flatNumber(flatNumber)
                .role(role)
                .registeredAt(LocalDateTime.now())
                .build());
        return 1;
    }
}
