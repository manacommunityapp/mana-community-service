package com.manacommunity.api.service.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.dto.scheduler.SportsRaceResultRequest;
import com.manacommunity.api.dto.scheduler.SportsRaceResultResponse;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.SportsAuctionPlayer;
import com.manacommunity.api.model.SportsAuctionTeam;
import com.manacommunity.api.model.scheduler.*;
import com.manacommunity.api.repository.SportsAuctionPlayerRepository;
import com.manacommunity.api.repository.SportsAuctionTeamRepository;
import com.manacommunity.api.repository.scheduler.SportsRaceResultRepository;
import com.manacommunity.api.repository.scheduler.SportsTournamentMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsRaceResultService {

    private final SportsRaceResultRepository raceRepo;
    private final SportsTournamentMatchRepository matchRepo;
    private final SportsAuctionPlayerRepository playerRepo;
    private final SportsAuctionTeamRepository teamRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public SportsRaceResultResponse recordResult(SportsRaceResultRequest req) {
        SportsTournamentMatch match = matchRepo.findById(req.matchId())
            .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentMatch", req.matchId()));

        SportsAuctionPlayer player = playerRepo.findById(req.playerId())
            .orElseThrow(() -> new ResourceNotFoundException("SportsAuctionPlayer", req.playerId()));

        SportsAuctionTeam team = req.teamId() != null
            ? teamRepo.findById(req.teamId()).orElse(null) : null;

        if (match.getStatus() != MatchStatus.LIVE) {
            match.setStatus(MatchStatus.LIVE);
            if (match.getStartedAt() == null) match.setStartedAt(java.time.LocalDateTime.now());
            matchRepo.save(match);
        }

        String splitTimesJson = null;
        if (req.splitTimes() != null && !req.splitTimes().isEmpty()) {
            try {
                splitTimesJson = objectMapper.writeValueAsString(req.splitTimes());
            } catch (Exception e) {
                log.warn("Failed to serialize split times", e);
            }
        }

        Long personalBest = getPersonalBest(req.playerId());
        boolean isPB = req.finishTimeMillis() != null && personalBest != null
            && req.finishTimeMillis() < personalBest;
        if (req.finishTimeMillis() != null && (personalBest == null || req.finishTimeMillis() < personalBest)) {
            personalBest = req.finishTimeMillis();
        }

        SportsRaceResult result = SportsRaceResult.builder()
            .match(match)
            .player(player)
            .team(team)
            .heatNumber(req.heatNumber())
            .laneNumber(req.laneNumber())
            .finishTimeMillis(req.finishTimeMillis())
            .formattedTime(req.formattedTime() != null ? req.formattedTime() : formatTime(req.finishTimeMillis()))
            .raceStatus(req.raceStatus() != null ? RaceStatus.valueOf(req.raceStatus()) : RaceStatus.FINISHED)
            .personalBestMillis(personalBest)
            .isPersonalBest(isPB)
            .splitTimes(splitTimesJson)
            .notes(req.notes())
            .build();

        result = raceRepo.save(result);
        updateRanks(req.matchId());

        result = raceRepo.findById(result.getId()).orElse(result);
        return toResponse(result);
    }

    @Transactional(readOnly = true)
    public List<SportsRaceResultResponse> getResults(Long matchId) {
        return raceRepo.findByMatchIdOrderByOverallRankAsc(matchId).stream()
            .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SportsRaceResultResponse> getHeatResults(Long matchId, Integer heatNumber) {
        return raceRepo.findByMatchIdAndHeatNumberOrderByHeatRankAsc(matchId, heatNumber).stream()
            .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SportsRaceResultResponse> getPlayerRaceHistory(Long playerId) {
        return raceRepo.findByPlayerIdOrderByCreatedAtDesc(playerId).stream()
            .map(this::toResponse).toList();
    }

    @Transactional
    public void updateRanks(Long matchId) {
        List<SportsRaceResult> all = raceRepo.findByMatchIdOrderByOverallRankAsc(matchId);

        List<SportsRaceResult> finished = all.stream()
            .filter(r -> r.getRaceStatus() == RaceStatus.FINISHED && r.getFinishTimeMillis() != null)
            .sorted(Comparator.comparingLong(SportsRaceResult::getFinishTimeMillis))
            .toList();

        for (int i = 0; i < finished.size(); i++) {
            finished.get(i).setOverallRank(i + 1);
        }

        Map<Integer, List<SportsRaceResult>> byHeat = all.stream()
            .filter(r -> r.getHeatNumber() != null && r.getRaceStatus() == RaceStatus.FINISHED && r.getFinishTimeMillis() != null)
            .sorted(Comparator.comparingLong(SportsRaceResult::getFinishTimeMillis))
            .collect(Collectors.groupingBy(SportsRaceResult::getHeatNumber, LinkedHashMap::new, Collectors.toList()));

        byHeat.forEach((heat, results) -> {
            for (int i = 0; i < results.size(); i++) {
                results.get(i).setHeatRank(i + 1);
            }
        });

        raceRepo.saveAll(all);
    }

    private Long getPersonalBest(Long playerId) {
        return raceRepo.findByPlayerIdOrderByCreatedAtDesc(playerId).stream()
            .filter(r -> r.getRaceStatus() == RaceStatus.FINISHED && r.getFinishTimeMillis() != null)
            .map(SportsRaceResult::getFinishTimeMillis)
            .min(Long::compareTo)
            .orElse(null);
    }

    private String formatTime(Long millis) {
        if (millis == null) return null;
        long totalSeconds = millis / 1000;
        long ms = millis % 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        if (minutes > 0) {
            return String.format("%d:%02d.%03d", minutes, seconds, ms);
        }
        return String.format("%d.%03d", seconds, ms);
    }

    private SportsRaceResultResponse toResponse(SportsRaceResult r) {
        List<String> splits = null;
        if (r.getSplitTimes() != null) {
            try {
                splits = objectMapper.readValue(r.getSplitTimes(), new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("Failed to parse split times", e);
            }
        }
        return new SportsRaceResultResponse(
            r.getId(),
            r.getMatch().getId(),
            r.getPlayer().getId(),
            r.getPlayer().getPlayerName(),
            r.getTeam() != null ? r.getTeam().getId() : null,
            r.getTeam() != null ? r.getTeam().getTeamName() : null,
            r.getHeatNumber(),
            r.getLaneNumber(),
            r.getFinishTimeMillis(),
            r.getFormattedTime(),
            r.getRaceStatus() != null ? r.getRaceStatus().name() : null,
            r.getOverallRank(),
            r.getHeatRank(),
            r.getPersonalBestMillis(),
            r.getIsPersonalBest(),
            splits,
            r.getNotes()
        );
    }
}
