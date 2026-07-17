package com.manacommunity.api.repository;

import com.manacommunity.api.model.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    Optional<PollVote> findByPostIdAndUserId(Long postId, Long userId);
    List<PollVote> findByPostId(Long postId);

    @Query("SELECT pv.selectedOption, COUNT(pv) FROM PollVote pv WHERE pv.post.id = :postId GROUP BY pv.selectedOption")
    List<Object[]> countVotesGroupByOption(@Param("postId") Long postId);
}
