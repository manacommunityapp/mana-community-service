package com.manacommunity.api.homeservices.repository;

import com.manacommunity.api.homeservices.entity.HomeServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeServiceCategoryRepository extends JpaRepository<HomeServiceCategory, Long> {
    List<HomeServiceCategory> findByIsActiveTrue();
}
