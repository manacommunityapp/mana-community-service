package com.manacommunity.api.repository;

import com.manacommunity.api.model.SportsTournamentGalleryImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsTournamentGalleryImageRepository extends JpaRepository<SportsTournamentGalleryImage, Long> {

    List<SportsTournamentGalleryImage> findByTournamentIdOrderBySortOrderAscIdAsc(Long tournamentId);
}
