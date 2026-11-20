package com.manacommunity.api.sportsops.service;

import com.manacommunity.api.sportsops.entity.TournamentSponsor;
import com.manacommunity.api.sportsops.repository.TournamentSponsorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SponsorService {
    private final TournamentSponsorRepository repo;

    public List<TournamentSponsor> getForTournament(Long tournamentId) {
        return repo.findByTournamentId(tournamentId);
    }

    @Transactional
    public TournamentSponsor add(TournamentSponsor sponsor) {
        return repo.save(sponsor);
    }

    @Transactional
    public TournamentSponsor updateStatus(Long id, String status) {
        TournamentSponsor s = repo.findById(id).orElseThrow();
        s.setStatus(status);
        return repo.save(s);
    }
}
