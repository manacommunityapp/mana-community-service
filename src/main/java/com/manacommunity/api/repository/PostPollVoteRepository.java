package com.manacommunity.api.repository;

import com.manacommunity.api.model.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

/**
 * Votes on feed/post polls (over {@link PollVote}, keyed by post + user).
 *
 * Renamed from PollVoteRepository to avoid a Spring Data bean-name clash with
 * com.manacommunity.api.polling.repository.PollVoteRepository — Spring derives
 * the bean name from the interface's simple name, so two "PollVoteRepository"
 * interfaces both resolve to "pollVoteRepository" and collide.
 */
public interface PostPollVoteRepository extends JpaRepository<PollVote, Long> {
    Optional<PollVote> findByPostIdAndUserId(Long postId, Long userId);
    List<PollVote> findByPostId(Long postId);

    @Query("SELECT pv.selectedOption, COUNT(pv) FROM PollVote pv WHERE pv.post.id = :postId GROUP BY pv.selectedOption")
    List<Object[]> countVotesGroupByOption(@Param("postId") Long postId);
}
