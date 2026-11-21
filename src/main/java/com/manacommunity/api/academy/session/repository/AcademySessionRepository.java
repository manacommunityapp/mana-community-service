package com.manacommunity.api.academy.session.repository;

import com.manacommunity.api.academy.session.entity.AcademySession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademySessionRepository extends JpaRepository<AcademySession, Long> {
    Page<AcademySession> findByCommunityIdAndStatusInOrderByStartsAtAsc(
            Long communityId, List<String> statuses, Pageable pageable);
    Page<AcademySession> findByCommunityIdAndCategoryAndStatusInOrderByStartsAtAsc(
            Long communityId, String category, List<String> statuses, Pageable pageable);
    List<AcademySession> findByInstructorIdOrderByStartsAtDesc(Long instructorId);
}
