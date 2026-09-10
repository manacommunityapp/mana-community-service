package com.manacommunity.api.service;

import com.manacommunity.api.dto.SportsTournamentRequest;
import com.manacommunity.api.model.SportsEvent;
import com.manacommunity.api.model.SportsTournament;
import java.util.List;

public interface SportsTournamentService {
    List<SportsTournament> getAllTournaments();
    List<SportsTournament> getCommunityTournaments(Long communityId);
    SportsTournament getTournamentById(Long id);
    void deleteTournament(Long id);
    SportsTournament saveTournamentRecord(SportsTournamentRequest req, Boolean allowAdminChat);
    /** Update an existing tournament in place (identified by id). Never inserts a new record. */
    SportsTournament updateTournamentRecord(Long id, SportsTournamentRequest req, Boolean allowAdminChat);
    SportsTournament updateStatus(Long id, String status);
}
