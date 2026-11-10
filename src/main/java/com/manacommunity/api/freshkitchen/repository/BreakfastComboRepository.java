package com.manacommunity.api.freshkitchen.repository;

import com.manacommunity.api.freshkitchen.model.BreakfastCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BreakfastComboRepository extends JpaRepository<BreakfastCombo, Long> {

    Optional<BreakfastCombo> findByComboCode(String comboCode);

    Optional<BreakfastCombo> findByComboCodeAndIsActiveTrue(String comboCode);

    List<BreakfastCombo> findByIsActiveTrue();
}
