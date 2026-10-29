package com.manacommunity.api.sports.service.impl;

import com.manacommunity.api.sports.model.SportsAuctionPlayer;
import com.manacommunity.api.sports.repository.SportsAuctionPlayerRepository;
import com.manacommunity.api.sports.service.SportsAuctionPlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SportsAuctionPlayerServiceImpl implements SportsAuctionPlayerService {

    private final SportsAuctionPlayerRepository auctionPlayerRepository;

    @Override
    public SportsAuctionPlayer savePlayer(SportsAuctionPlayer player) {
        return auctionPlayerRepository.save(player);
    }
}