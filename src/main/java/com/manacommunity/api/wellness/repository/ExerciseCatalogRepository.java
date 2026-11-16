package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.ExerciseCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseCatalogRepository extends JpaRepository<ExerciseCatalog, Long> {
    List<ExerciseCatalog> findByNameContainingIgnoreCaseOrNameLocalContainingIgnoreCase(String name, String nameLocal);
    List<ExerciseCatalog> findByCategoryIgnoreCase(String category);
}
