package com.manacommunity.api.sports.service.impl;

import com.manacommunity.api.sports.model.SportsAuctionPlayer;
import com.manacommunity.api.sports.repository.SportsAuctionPlayerRepository;
import com.manacommunity.api.sports.service.SportsAuctionPlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SportsAuctionPlayerServiceImpl implements SportsAuctionPlayerService {

    private final SportsAuctionPlayerRepository auctionPlayerRepository;

    @Override
    public SportsAuctionPlayer savePlayer(SportsAuctionPlayer player) {
        return auctionPlayerRepository.save(player);
    }
}