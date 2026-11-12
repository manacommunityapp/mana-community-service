package com.manacommunity.api.mw.repository;

import com.manacommunity.api.mw.entity.MwSkillPassport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MwSkillPassportRepository extends JpaRepository<MwSkillPassport, Long> {
    Optional<MwSkillPassport> findByManaUserId(Long manaUserId);
    List<MwSkillPassport> findByIsPublicTrue();
}
