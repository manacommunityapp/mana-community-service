package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.SportsPlayerRankingRequest;
import com.manacommunity.api.dto.SportsPlayerRankingResponse;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.SportsMeta;
import com.manacommunity.api.model.SportsPlayerRanking;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.SportsMetaRepository;
import com.manacommunity.api.repository.SportsPlayerRankingRepository;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.service.SportsPlayerRankingService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SportsPlayerRankingServiceImpl implements SportsPlayerRankingService {

    private final SportsPlayerRankingRepository rankingRepo;
    private final AppUserRepository userRepo;
    private final SportsMetaRepository sportRepo;
    private final CommunityRepository communityRepo;

    @Override
    @Transactional
    public SportsPlayerRankingResponse upsert(SportsPlayerRankingRequest req) {
        String season = req.resolvedSeason();
        SportsPlayerRanking ranking = rankingRepo
                .findByUserIdAndSportIdAndCommunityIdAndSeason(req.userId(), req.sportId(), req.communityId(), season)
                .orElseGet(() -> {
                    AppUser user = userRepo.findById(req.userId())
                            .orElseThrow(() -> new ResourceNotFoundException("AppUser", req.userId()));
                    SportsMeta sport = sportRepo.findById(req.sportId())
                            .orElseThrow(() -> new ResourceNotFoundException("SportsMeta", req.sportId()));
                    Community community = communityRepo.findById(req.communityId())
                            .orElseThrow(() -> new ResourceNotFoundException("Community", req.communityId()));
                    return SportsPlayerRanking.builder()
                            .user(user)
                            .sport(sport)
                            .community(community)
                            .season(season)
                            .build();
                });

        ranking.setRank(req.rank());
        ranking.setRating(req.rating());
        ranking.setNotes(req.notes());
        ranking.setSource(SportsPlayerRanking.RankingSource.MANUAL);

        return toResponse(rankingRepo.save(ranking));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsPlayerRankingResponse> listBySportAndCommunity(Long sportId, Long communityId, String season) {
        String s = (season == null || season.isBlank()) ? "CURRENT" : season;
        return rankingRepo.findBySportAndCommunityAndSeason(sportId, communityId, s)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsPlayerRankingResponse> listByUserAndCommunity(Long userId, Long communityId, String season) {
        String s = (season == null || season.isBlank()) ? "CURRENT" : season;
        return rankingRepo.findByUserAndCommunityAndSeason(userId, communityId, s)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long rankingId) {
        if (!rankingRepo.existsById(rankingId)) {
            throw new ResourceNotFoundException("SportsPlayerRanking", rankingId);
        }
        rankingRepo.deleteById(rankingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> getRating(Long userId, Long sportId, Long communityId) {
        return rankingRepo.findByUserIdAndSportIdAndCommunityId(userId, sportId, communityId)
                .map(SportsPlayerRanking::getRating);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> getSeed(Long userId, Long sportId, Long communityId, String season) {
        String s = (season == null || season.isBlank()) ? "CURRENT" : season;
        return rankingRepo.findByUserIdAndSportIdAndCommunityIdAndSeason(userId, sportId, communityId, s)
                .map(SportsPlayerRanking::getRank);
    }

    private SportsPlayerRankingResponse toResponse(SportsPlayerRanking r) {
        AppUser u = r.getUser();
        SportsMeta sp = r.getSport();
        return SportsPlayerRankingResponse.builder()
                .id(r.getId())
                .player(SportsPlayerRankingResponse.PlayerRef.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .flatNo(u.getFlatNo())
                        .build())
                .sport(SportsPlayerRankingResponse.SportRef.builder()
                        .id(sp.getId())
                        .name(sp.getName())
                        .icon(sp.getIcon())
                        .build())
                .communityId(r.getCommunity().getId())
                .rank(r.getRank())
                .rating(r.getRating())
                .source(r.getSource().name())
                .season(r.getSeason())
                .notes(r.getNotes())
                .build();
    }
}
