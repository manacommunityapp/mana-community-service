package com.manacommunity.api.polling.unit;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.polling.dto.PollRequest;
import com.manacommunity.api.polling.dto.PollResponse;
import com.manacommunity.api.polling.entity.Poll;
import com.manacommunity.api.polling.entity.PollOption;
import com.manacommunity.api.polling.entity.PollVote;
import com.manacommunity.api.polling.repository.PollRepository;
import com.manacommunity.api.polling.repository.PollVoteRepository;
import com.manacommunity.api.polling.service.PollService;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PollService Unit Tests")
class PollServiceTest {

    @Mock
    private PollRepository pollRepo;

    @Mock
    private PollVoteRepository voteRepo;

    @InjectMocks
    private PollService pollService;

    private Community community;
    private AppUser creator;
    private AppUser voter;
    private Poll singleChoicePoll;
    private Poll multiChoicePoll;
    private PollOption opt1;
    private PollOption opt2;
    private PollOption opt3;

    @BeforeEach
    void setUp() {
        community = Community.builder().id(100L).name("Mana Foresta").build();
        creator = AppUser.builder().id(1L).fullName("Admin User").community(community).build();
        voter = AppUser.builder().id(2L).fullName("Resident Voter").community(community).build();

        opt1 = PollOption.builder().id(10L).text("Option A").sortOrder(0).build();
        opt2 = PollOption.builder().id(20L).text("Option B").sortOrder(1).build();
        opt3 = PollOption.builder().id(30L).text("Option C").sortOrder(2).build();

        singleChoicePoll = Poll.builder()
                .id(1000L)
                .question("Which color for the clubhouse?")
                .description("Annual repainting")
                .closesOn(LocalDate.now().plusDays(7))
                .allowMultiple(false)
                .anonymous(false)
                .createdBy(creator)
                .community(community)
                .options(new ArrayList<>(List.of(opt1, opt2, opt3)))
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
        opt1.setPoll(singleChoicePoll);
        opt2.setPoll(singleChoicePoll);
        opt3.setPoll(singleChoicePoll);

        multiChoicePoll = Poll.builder()
                .id(2000L)
                .question("Which amenities do you use?")
                .description("Select all that apply")
                .closesOn(LocalDate.now().plusDays(14))
                .allowMultiple(true)
                .anonymous(true)
                .createdBy(creator)
                .community(community)
                .options(new ArrayList<>(List.of(opt1, opt2, opt3)))
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();
    }

    @Nested
    @DisplayName("Create Poll")
    class CreatePollTests {

        @Test
        @DisplayName("Creates single-choice poll with options")
        void createSingleChoicePoll() {
            PollRequest req = new PollRequest();
            req.setQuestion("New Gym Equipment?");
            req.setDescription("Treadmill vs Elliptical");
            req.setClosesOn(LocalDate.now().plusDays(5).toString());
            req.setAllowMultiple(false);
            req.setAnonymous(false);
            req.setOptions(List.of("Treadmill", "Elliptical", "Rowing Machine"));

            when(pollRepo.save(any(Poll.class))).thenAnswer(inv -> {
                Poll p = inv.getArgument(0);
                p.setId(3000L);
                return p;
            });
            when(voteRepo.findByPollId(3000L)).thenReturn(List.of());

            PollResponse res = pollService.create(req, creator, community);

            assertThat(res).isNotNull();
            assertThat(res.getId()).isEqualTo(3000L);
            assertThat(res.getQuestion()).isEqualTo("New Gym Equipment?");
            assertThat(res.isAllowMultiple()).isFalse();
            assertThat(res.getOptions()).hasSize(3);
            verify(pollRepo).save(any(Poll.class));
        }

        @Test
        @DisplayName("Creates multi-choice anonymous poll")
        void createMultiChoiceAnonymousPoll() {
            PollRequest req = new PollRequest();
            req.setQuestion("Preferred Weekend Sports?");
            req.setClosesOn(LocalDate.now().plusDays(10).toString());
            req.setAllowMultiple(true);
            req.setAnonymous(true);
            req.setOptions(List.of("Badminton", "Tennis", "Cricket"));

            when(pollRepo.save(any(Poll.class))).thenAnswer(inv -> {
                Poll p = inv.getArgument(0);
                p.setId(4000L);
                return p;
            });
            when(voteRepo.findByPollId(4000L)).thenReturn(List.of());

            PollResponse res = pollService.create(req, creator, community);

            assertThat(res).isNotNull();
            assertThat(res.isAllowMultiple()).isTrue();
            assertThat(res.isAnonymous()).isTrue();
        }
    }

    @Nested
    @DisplayName("Voting & Multi-choice Validation")
    class VotingTests {

        @Test
        @DisplayName("Single-choice poll: successful vote")
        void singleChoiceVoteSuccess() {
            when(pollRepo.findById(1000L)).thenReturn(Optional.of(singleChoicePoll));
            when(voteRepo.findByPollIdAndVoterId(1000L, voter.getId())).thenReturn(List.of());
            when(voteRepo.findByPollId(1000L)).thenReturn(List.of(
                    PollVote.builder().id(501L).poll(singleChoicePoll).option(opt1).voter(voter).build()
            ));

            PollResponse res = pollService.vote(1000L, List.of(10L), voter);

            assertThat(res).isNotNull();
            assertThat(res.isHasVoted()).isTrue();
            verify(voteRepo).save(any(PollVote.class));
        }

        @Test
        @DisplayName("Single-choice poll: rejects multiple options")
        void singleChoiceRejectsMultipleOptions() {
            when(pollRepo.findById(1000L)).thenReturn(Optional.of(singleChoicePoll));
            when(voteRepo.findByPollIdAndVoterId(1000L, voter.getId())).thenReturn(List.of());

            assertThatThrownBy(() -> pollService.vote(1000L, List.of(10L, 20L), voter))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("allows only one selection");
        }

        @Test
        @DisplayName("Multi-choice poll: allows multiple options")
        void multiChoiceAllowsMultipleOptions() {
            when(pollRepo.findById(2000L)).thenReturn(Optional.of(multiChoicePoll));
            when(voteRepo.findByPollIdAndVoterId(2000L, voter.getId())).thenReturn(List.of());
            when(voteRepo.findByPollId(2000L)).thenReturn(List.of(
                    PollVote.builder().id(501L).poll(multiChoicePoll).option(opt1).voter(voter).build(),
                    PollVote.builder().id(502L).poll(multiChoicePoll).option(opt2).voter(voter).build()
            ));

            PollResponse res = pollService.vote(2000L, List.of(10L, 20L), voter);

            assertThat(res).isNotNull();
            assertThat(res.isHasVoted()).isTrue();
            verify(voteRepo, times(2)).save(any(PollVote.class));
        }

        @Test
        @DisplayName("Re-voting replaces previous vote")
        void revotingReplacesPreviousVote() {
            PollVote oldVote = PollVote.builder().id(501L).poll(singleChoicePoll).option(opt1).voter(voter).build();
            when(pollRepo.findById(1000L)).thenReturn(Optional.of(singleChoicePoll));
            when(voteRepo.findByPollIdAndVoterId(1000L, voter.getId())).thenReturn(List.of(oldVote));
            when(voteRepo.findByPollId(1000L)).thenReturn(List.of(
                    PollVote.builder().id(502L).poll(singleChoicePoll).option(opt2).voter(voter).build()
            ));

            PollResponse res = pollService.vote(1000L, List.of(20L), voter);

            verify(voteRepo).deleteAll(List.of(oldVote));
            verify(voteRepo).save(any(PollVote.class));
            assertThat(res.isHasVoted()).isTrue();
        }

        @Test
        @DisplayName("Voting on invalid option throws IllegalArgumentException")
        void voteInvalidOptionThrows() {
            when(pollRepo.findById(1000L)).thenReturn(Optional.of(singleChoicePoll));
            when(voteRepo.findByPollIdAndVoterId(1000L, voter.getId())).thenReturn(List.of());

            assertThatThrownBy(() -> pollService.vote(1000L, List.of(999L), voter))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid option: 999");
        }
    }

    @Nested
    @DisplayName("Expiry Management")
    class ExpiryManagementTests {

        @Test
        @DisplayName("Voting on expired poll throws IllegalStateException")
        void expiredPollRejectsVoting() {
            Poll expiredPoll = Poll.builder()
                    .id(5000L)
                    .question("Old Poll")
                    .closesOn(LocalDate.now().minusDays(1)) // expired yesterday
                    .allowMultiple(false)
                    .createdBy(creator)
                    .community(community)
                    .options(List.of(opt1))
                    .build();

            when(pollRepo.findById(5000L)).thenReturn(Optional.of(expiredPoll));

            assertThatThrownBy(() -> pollService.vote(5000L, List.of(10L), voter))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("This poll has closed");
        }

        @Test
        @DisplayName("Response marks closed poll with closed=true and reveals results")
        void expiredPollMarksClosedInResponse() {
            Poll expiredPoll = Poll.builder()
                    .id(5000L)
                    .question("Old Poll")
                    .closesOn(LocalDate.now().minusDays(2))
                    .allowMultiple(false)
                    .createdBy(creator)
                    .community(community)
                    .options(List.of(opt1, opt2))
                    .build();

            when(pollRepo.findById(5000L)).thenReturn(Optional.of(expiredPoll));
            when(voteRepo.findByPollId(5000L)).thenReturn(List.of(
                    PollVote.builder().id(101L).poll(expiredPoll).option(opt1).voter(creator).build()
            ));

            PollResponse res = pollService.getById(5000L, voter.getId());

            assertThat(res.isClosed()).isTrue();
            // Results should be shown to non-voter because poll is closed
            assertThat(res.getOptions().get(0).getVoteCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Querying & Deletion")
    class QueryAndDeletionTests {

        @Test
        @DisplayName("getActivePolls returns community polls")
        void getActivePolls() {
            when(pollRepo.findActiveByCommunity(100L)).thenReturn(List.of(singleChoicePoll));
            when(voteRepo.findByPollId(1000L)).thenReturn(List.of());

            List<PollResponse> result = pollService.getActivePolls(100L, voter.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1000L);
        }

        @Test
        @DisplayName("Creator can delete poll")
        void creatorCanDelete() {
            when(pollRepo.findById(1000L)).thenReturn(Optional.of(singleChoicePoll));
            when(voteRepo.findByPollId(1000L)).thenReturn(List.of());

            pollService.delete(1000L, creator.getId());

            verify(pollRepo).delete(singleChoicePoll);
        }

        @Test
        @DisplayName("Non-creator cannot delete poll")
        void nonCreatorCannotDelete() {
            when(pollRepo.findById(1000L)).thenReturn(Optional.of(singleChoicePoll));

            assertThatThrownBy(() -> pollService.delete(1000L, voter.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only the creator can delete this poll");
        }
    }
}
