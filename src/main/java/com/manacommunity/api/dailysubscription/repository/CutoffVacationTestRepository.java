package com.manacommunity.api.dailysubscription.repository;

import com.manacommunity.api.dailysubscription.entity.CutoffVacationTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CutoffVacationTestRepository extends JpaRepository<CutoffVacationTest, Long> {}
