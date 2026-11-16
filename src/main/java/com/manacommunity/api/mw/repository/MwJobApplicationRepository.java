package com.manacommunity.api.mw.repository;

import com.manacommunity.api.mw.entity.MwJobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MwJobApplicationRepository extends JpaRepository<MwJobApplication, Long> {

    List<MwJobApplication> findByJob_Id(Long jobId);

    List<MwJobApplication> findByManaUserId(Long manaUserId);

    Optional<MwJobApplication> findByJob_IdAndManaUserId(Long jobId, Long manaUserId);
}
