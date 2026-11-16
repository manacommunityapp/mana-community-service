package com.manacommunity.api.mw.repository;

import com.manacommunity.api.mw.entity.MwPassportSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MwPassportSkillRepository extends JpaRepository<MwPassportSkill, Long> {
    List<MwPassportSkill> findByPassportId(Long passportId);
    List<MwPassportSkill> findByPassportIdAndSkillNameContainingIgnoreCase(Long passportId, String skill);
}
