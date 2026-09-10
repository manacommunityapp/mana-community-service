package com.manacommunity.api.service.impl;

import com.manacommunity.api.model.SportsAuctionPlayer;
import com.manacommunity.api.repository.SportsAuctionPlayerRepository;
import com.manacommunity.api.service.SportsAuctionPlayerService;
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