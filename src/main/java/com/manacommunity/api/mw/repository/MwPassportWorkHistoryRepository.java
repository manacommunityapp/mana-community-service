package com.manacommunity.api.mw.repository;

import com.manacommunity.api.mw.entity.MwPassportWorkHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MwPassportWorkHistoryRepository extends JpaRepository<MwPassportWorkHistory, Long> {
    List<MwPassportWorkHistory> findByPassportId(Long passportId);
}
