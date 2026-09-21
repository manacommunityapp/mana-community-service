package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.dashboard.SportsAnalyticsResponse;
import com.manacommunity.api.dto.dashboard.SportsAnalyticsResponse.*;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.service.SportsAnalyticsService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.manacommunity.api.constants.PermissionConstants.ROLE_SUPER_ADMIN;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsAnalyticsServiceImpl implements SportsAnalyticsService {

    private final SportsTournamentRepository tournamentRepo;
    private final SportsEventRepository eventRepo;
    private final SportsEventRegistrationRepository registrationRepo;
    private final VenueRepository venueRepo;
    private final SportsAuctionConfigRepository auctionConfigRepo;
    private final SportsAuctionTeamRepository auctionTeamRepo;
    private final SportsAuctionPlayerRepository auctionPlayerRepo;

    @Override
    @Transactional(readOnly = true)
    public SportsAnalyticsResponse getAnalytics(AppUser user, Long requestedCommunityId) {
        boolean isSuperAdmin = user != null && user.hasRole(ROLE_SUPER_ADMIN);
        Long targetCommunityId = isSuperAdmin ? requestedCommunityId
                : (user != null && user.getCommunity() != null ? user.getCommunity().getId() : requestedCommunityId);

        // 1. Fetch Tournaments
        List<SportsTournament> tournaments = targetCommunityId == null
                ? tournamentRepo.findAll()
                : tournamentRepo.findByEventCommunityIdOrderByCreatedAtDesc(targetCommunityId);

        long activeTournaments = tournaments.stream()
                .filter(t -> t.getRegistrationStatus() == SportsEventStatus.REGISTRATION_OPEN
                        || t.getRegistrationStatus() == SportsEventStatus.LIVE)
                .count();

        // 2. Fetch Events
        List<SportsEvent> events = targetCommunityId == null
                ? eventRepo.findAll()
                : eventRepo.findByCommunityIdOrderByEventDateStartDesc(targetCommunityId);

        long activeEvents = events.stream()
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .count();

        // 3. Fetch Registrations
        List<SportsEventRegistration> registrations = targetCommunityId == null
                ? registrationRepo.findAll()
                : registrationRepo.findByCommunityId(targetCommunityId);

        long totalRegistrations = registrations.size();
        long confirmedRegistrations = registrations.stream()
                .filter(r -> r.getStatus() == SportsEventRegistration.RegistrationStatus.CONFIRMED)
                .count();
        long pendingRegistrations = registrations.stream()
                .filter(r -> r.getStatus() == SportsEventRegistration.RegistrationStatus.REGISTERED
                        || r.getStatus() == SportsEventRegistration.RegistrationStatus.PENDING)
                .count();

        // Distinct participants
        Set<String> uniqueParticipantKeys = new HashSet<>();
        for (SportsEventRegistration r : registrations) {
            if (r.getUser() != null) {
                uniqueParticipantKeys.add("U-" + r.getUser().getId());
            } else if (r.getFamilyMember() != null) {
                uniqueParticipantKeys.add("FM-" + r.getFamilyMember().getId());
            } else if (r.getPlayerName() != null) {
                uniqueParticipantKeys.add("NAME-" + r.getPlayerName());
            }
        }
        long uniqueParticipants = uniqueParticipantKeys.size();

        // 4. Fetch Venues
        List<Venue> venues = targetCommunityId == null
                ? venueRepo.findAll()
                : venueRepo.findAll().stream()
                .filter(v -> v.getCommunity() != null && targetCommunityId.equals(v.getCommunity().getId()))
                .toList();

        OverviewMetrics overview = new OverviewMetrics(
                tournaments.size(),
                activeTournaments,
                events.size(),
                activeEvents,
                totalRegistrations,
                confirmedRegistrations,
                pendingRegistrations,
                uniqueParticipants,
                venues.size()
        );

        // 5. Sport Participation Breakdown
        Map<Long, List<SportsEvent>> eventsBySport = events.stream()
                .filter(e -> e.getSport() != null)
                .collect(Collectors.groupingBy(e -> e.getSport().getId()));

        Map<Long, List<SportsEventRegistration>> regBySport = registrations.stream()
                .filter(r -> r.getEvent() != null && r.getEvent().getSport() != null)
                .collect(Collectors.groupingBy(r -> r.getEvent().getSport().getId()));

        Set<Long> allSportIds = new HashSet<>();
        allSportIds.addAll(eventsBySport.keySet());
        allSportIds.addAll(regBySport.keySet());

        List<SportParticipationMetric> sportParticipation = new ArrayList<>();
        for (Long sportId : allSportIds) {
            List<SportsEvent> sportEvents = eventsBySport.getOrDefault(sportId, List.of());
            List<SportsEventRegistration> sportRegs = regBySport.getOrDefault(sportId, List.of());
            SportsMeta sportMeta = !sportEvents.isEmpty() ? sportEvents.get(0).getSport()
                    : (!sportRegs.isEmpty() ? sportRegs.get(0).getEvent().getSport() : null);

            String sportName = sportMeta != null ? sportMeta.getName() : "Sport #" + sportId;
            String icon = sportMeta != null ? sportMeta.getIcon() : "🏅";
            long sConfirmed = sportRegs.stream()
                    .filter(r -> r.getStatus() == SportsEventRegistration.RegistrationStatus.CONFIRMED)
                    .count();

            sportParticipation.add(new SportParticipationMetric(
                    sportId, sportName, icon, sportEvents.size(), sportRegs.size(), sConfirmed
            ));
        }
        sportParticipation.sort((a, b) -> Long.compare(b.totalRegistrations(), a.totalRegistrations()));

        // 6. Registration Status Breakdown
        Map<SportsEventRegistration.RegistrationStatus, Long> statusCounts = registrations.stream()
                .filter(r -> r.getStatus() != null)
                .collect(Collectors.groupingBy(SportsEventRegistration::getStatus, Collectors.counting()));

        List<RegistrationStatusMetric> registrationStatuses = new ArrayList<>();
        for (Map.Entry<SportsEventRegistration.RegistrationStatus, Long> entry : statusCounts.entrySet()) {
            double pct = totalRegistrations > 0 ? (entry.getValue() * 100.0 / totalRegistrations) : 0.0;
            registrationStatuses.add(new RegistrationStatusMetric(
                    entry.getKey().name(), entry.getValue(), Math.round(pct * 10.0) / 10.0
            ));
        }

        // 7. Gender Breakdown
        long maleCount = 0;
        long femaleCount = 0;
        long otherCount = 0;
        for (SportsEventRegistration r : registrations) {
            String gender = null;
            if (r.getUser() != null && r.getUser().getGender() != null) {
                gender = r.getUser().getGender();
            } else if (r.getFamilyMember() != null && r.getFamilyMember().getGender() != null) {
                gender = r.getFamilyMember().getGender();
            }
            if (gender != null) {
                if (gender.equalsIgnoreCase("MALE") || gender.equalsIgnoreCase("M")) {
                    maleCount++;
                } else if (gender.equalsIgnoreCase("FEMALE") || gender.equalsIgnoreCase("F")) {
                    femaleCount++;
                } else {
                    otherCount++;
                }
            }
        }
        long totalGenderKnown = maleCount + femaleCount + otherCount;
        double malePct = totalGenderKnown > 0 ? (maleCount * 100.0 / totalGenderKnown) : 0.0;
        double femalePct = totalGenderKnown > 0 ? (femaleCount * 100.0 / totalGenderKnown) : 0.0;
        GenderBreakdown genderBreakdown = new GenderBreakdown(
                maleCount, femaleCount, otherCount,
                Math.round(malePct * 10.0) / 10.0,
                Math.round(femalePct * 10.0) / 10.0
        );

        // 8. Age Group Breakdown
        long kidsU12 = 0;
        long youth13_19 = 0;
        long adults20_50 = 0;
        long seniors50Plus = 0;

        for (SportsEventRegistration r : registrations) {
            Integer age = r.getAge();
            if (age == null && r.getFamilyMember() != null) {
                age = r.getFamilyMember().getAge();
            }
            if (age != null && age > 0) {
                if (age <= 12) kidsU12++;
                else if (age <= 19) youth13_19++;
                else if (age <= 50) adults20_50++;
                else seniors50Plus++;
            }
        }
        AgeGroupBreakdown ageGroupBreakdown = new AgeGroupBreakdown(
                kidsU12, youth13_19, adults20_50, seniors50Plus
        );

        // 9. Top Tournaments Metric
        Map<Long, List<SportsEventRegistration>> regByEvent = registrations.stream()
                .filter(r -> r.getEvent() != null)
                .collect(Collectors.groupingBy(r -> r.getEvent().getId()));

        List<TournamentMetric> topTournaments = new ArrayList<>();
        for (SportsTournament t : tournaments) {
            long tRegCount = 0;
            long tConfirmedCount = 0;
            int maxCap = t.getMaxParticipants() != null ? t.getMaxParticipants() : 0;

            if (t.getSportsEvents() != null) {
                for (SportsEvent se : t.getSportsEvents()) {
                    List<SportsEventRegistration> eRegs = regByEvent.getOrDefault(se.getId(), List.of());
                    tRegCount += eRegs.size();
                    tConfirmedCount += eRegs.stream()
                            .filter(r -> r.getStatus() == SportsEventRegistration.RegistrationStatus.CONFIRMED)
                            .count();
                }
            }
            double occupancy = maxCap > 0 ? (tConfirmedCount * 100.0 / maxCap) : 0.0;
            topTournaments.add(new TournamentMetric(
                    t.getId(),
                    t.getName(),
                    t.getRegistrationStatus() != null ? t.getRegistrationStatus().name() : "UNKNOWN",
                    maxCap,
                    tRegCount,
                    tConfirmedCount,
                    Math.round(occupancy * 10.0) / 10.0
            ));
        }
        topTournaments.sort((a, b) -> Long.compare(b.totalRegistrations(), a.totalRegistrations()));

        // 10. Venue Utilization
        Map<Long, Long> eventsByVenue = events.stream()
                .filter(e -> e.getVenue() != null)
                .collect(Collectors.groupingBy(e -> e.getVenue().getId(), Collectors.counting()));

        List<VenueUtilizationMetric> venueUtilization = new ArrayList<>();
        for (Venue v : venues) {
            long eCount = eventsByVenue.getOrDefault(v.getId(), 0L);
            long courtCount = v.getCourts() != null ? v.getCourts().size() : 0;
            venueUtilization.add(new VenueUtilizationMetric(v.getId(), v.getName(), eCount, courtCount));
        }

        // 11. Auction Analytics
        List<SportsAuctionConfig> auctionConfigs = targetCommunityId == null
                ? auctionConfigRepo.findAll()
                : auctionConfigRepo.findByCreatedByCommunityIdOrderByCreatedAtDesc(targetCommunityId);

        long totalTeams = 0;
        long totalPlayers = 0;
        long totalBudget = 0;
        long totalSpent = 0;

        for (SportsAuctionConfig cfg : auctionConfigs) {
            List<SportsAuctionTeam> teams = auctionTeamRepo.findByConfigIdOrderByTeamName(cfg.getId());
            totalTeams += teams.size();
            for (SportsAuctionTeam team : teams) {
                totalBudget += (team.getTotalBudget() != null ? team.getTotalBudget() : 0L);
                totalSpent += (team.getSpent() != null ? team.getSpent() : 0L);
            }
            totalPlayers += auctionPlayerRepo.countByConfigId(cfg.getId());
        }

        AuctionAnalytics auctionSummary = new AuctionAnalytics(
                auctionConfigs.size(), totalTeams, totalPlayers, totalBudget, totalSpent
        );

        return new SportsAnalyticsResponse(
                overview,
                sportParticipation,
                registrationStatuses,
                genderBreakdown,
                ageGroupBreakdown,
                topTournaments,
                venueUtilization,
                auctionSummary
        );
    }
}
