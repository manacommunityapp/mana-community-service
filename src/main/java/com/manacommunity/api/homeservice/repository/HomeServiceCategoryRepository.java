package com.manacommunity.api.homeservice.repository;

import com.manacommunity.api.homeservice.model.entity.HomeServiceCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository("homeServiceCategoryRepository")
public interface HomeServiceCategoryRepository extends JpaRepository<HomeServiceCategoryEntity, String> {
    List<HomeServiceCategoryEntity> findByActiveTrueOrderByDisplayOrderAsc();
    Optional<HomeServiceCategoryEntity> findByCode(String code);
}
