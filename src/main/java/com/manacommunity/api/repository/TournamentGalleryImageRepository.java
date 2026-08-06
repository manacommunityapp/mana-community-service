package com.manacommunity.api.repository;

import com.manacommunity.api.model.TournamentGalleryImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentGalleryImageRepository extends JpaRepository<TournamentGalleryImage, Long> {

    List<TournamentGalleryImage> findByTournamentIdOrderBySortOrderAscIdAsc(Long tournamentId);
}
