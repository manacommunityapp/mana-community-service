package com.manacommunity.api.fresh.city.repository;

import com.manacommunity.api.fresh.city.model.FreshCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreshCityRepository extends JpaRepository<FreshCity, Long> {
    Optional<FreshCity> findByCode(String code);
    List<FreshCity> findByIsActiveTrue();
}
