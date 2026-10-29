package com.manacommunity.api.academy.repository;

import com.manacommunity.api.academy.entity.AcademyProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademyProgramRepository extends JpaRepository<AcademyProgram, Long> {
    List<AcademyProgram> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<AcademyProgram> findByCommunityIdAndCategoryOrderByCreatedAtDesc(Long communityId, String category);
    List<AcademyProgram> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, String status);
    List<AcademyProgram> findByCommunityIdAndStatusNotOrderByCreatedAtDesc(Long communityId, String status);
    long countByCommunityIdAndStatus(Long communityId, String status);
}
