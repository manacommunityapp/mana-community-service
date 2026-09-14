package com.manacommunity.api.service;

import com.manacommunity.api.dto.SportsPlayerRankingRequest;
import com.manacommunity.api.dto.SportsPlayerRankingResponse;

import java.util.List;
import java.util.Optional;

public interface SportsPlayerRankingService {

    SportsPlayerRankingResponse upsert(SportsPlayerRankingRequest request);

    List<SportsPlayerRankingResponse> listBySportAndCommunity(Long sportId, Long communityId, String season);

    List<SportsPlayerRankingResponse> listByUserAndCommunity(Long userId, Long communityId, String season);

    void delete(Long rankingId);

    Optional<Integer> getRating(Long userId, Long sportId, Long communityId);

    Optional<Integer> getSeed(Long userId, Long sportId, Long communityId, String season);
}
