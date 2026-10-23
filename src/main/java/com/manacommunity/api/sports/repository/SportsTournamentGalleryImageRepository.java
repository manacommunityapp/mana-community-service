package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsTournamentGalleryImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsTournamentGalleryImageRepository extends JpaRepository<SportsTournamentGalleryImage, Long> {

    List<SportsTournamentGalleryImage> findByTournamentIdOrderBySortOrderAscIdAsc(Long tournamentId);
}
