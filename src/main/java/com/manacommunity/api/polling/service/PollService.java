package com.manacommunity.api.polling.service;

import com.manacommunity.api.governance.service.VotingEligibilityService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.polling.dto.PollRequest;
import com.manacommunity.api.polling.dto.PollResponse;
import com.manacommunity.api.polling.entity.Poll;
import com.manacommunity.api.polling.entity.PollOption;
import com.manacommunity.api.polling.entity.PollVote;
import com.manacommunity.api.polling.repository.PollRepository;
import com.manacommunity.api.polling.repository.PollVoteRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepo;
    private final PollVoteRepository voteRepo;

    @Autowired(required = false)
    private VotingEligibilityService eligibilityService;

    public void setEligibilityService(VotingEligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    @Transactional(readOnly = true)
    public List<PollResponse> getActivePolls(Long communityId, Long currentUserId) {
        return pollRepo.findActiveByCommunity(communityId)
                .stream().map(p -> toResponse(p, currentUserId)).toList();
    }

    @Transactional(readOnly = true)
    public List<PollResponse> getAllPolls(Long communityId, Long currentUserId) {
        return pollRepo.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream().map(p -> toResponse(p, currentUserId)).toList();
    }

    @Transactional(readOnly = true)
    public List<PollResponse> getMyPolls(Long userId) {
        return pollRepo.findByCreatedByIdOrderByCreatedAtDesc(userId)
                .stream().map(p -> toResponse(p, userId)).toList();
    }

    @Transactional(readOnly = true)
    public PollResponse getById(Long id, Long currentUserId) {
        Poll poll = pollRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found: " + id));
        return toResponse(poll, currentUserId);
    }

    @Transactional
    public PollResponse create(PollRequest req, AppUser user, Community community) {
        Poll poll = Poll.builder()
                .question(req.getQuestion())
                .description(req.getDescription())
                .closesOn(req.getClosesOn() != null ? LocalDate.parse(req.getClosesOn()) : null)
                .allowMultiple(req.isAllowMultiple())
                .anonymous(req.isAnonymous())
                .votingMode(req.getVotingMode() != null && !req.getVotingMode().isBlank()
                        ? req.getVotingMode()
                        : VotingEligibilityService.MODE_ONE_FLAT_ONE_VOTE)
                .excludeDefaultersOverDays(req.getExcludeDefaultersOverDays() != null
                        ? req.getExcludeDefaultersOverDays()
                        : 90)
                .ownersOnly(req.getOwnersOnly() == null || req.getOwnersOnly())
                .createdBy(user)
                .community(community)
                .build();

        for (int i = 0; i < req.getOptions().size(); i++) {
            PollOption opt = PollOption.builder()
                    .text(req.getOptions().get(i))
                    .sortOrder(i)
                    .poll(poll)
                    .build();
            poll.getOptions().add(opt);
        }

        return toResponse(pollRepo.save(poll), user.getId());
    }

    @Transactional
    public PollResponse vote(Long pollId, List<Long> optionIds, AppUser voter) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found: " + pollId));

        if (poll.getClosesOn() != null && poll.getClosesOn().isBefore(LocalDate.now())) {
            throw new IllegalStateException("This poll has closed");
        }

        List<PollVote> existing = voteRepo.findByPollIdAndVoterId(pollId, voter.getId());
        if (!existing.isEmpty()) {
            voteRepo.deleteAll(existing);
        }

        if (!poll.isAllowMultiple() && optionIds.size() > 1) {
            throw new IllegalStateException("This poll allows only one selection");
        }

        for (Long optionId : optionIds) {
            PollOption option = poll.getOptions().stream()
                    .filter(o -> o.getId().equals(optionId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid option: " + optionId));

            PollVote vote = PollVote.builder()
                    .poll(poll)
                    .option(option)
                    .voter(voter)
                    .build();
            voteRepo.save(vote);
        }

        return toResponse(pollRepo.findById(pollId).orElseThrow(), voter.getId());
    }

    @Transactional(readOnly = true)
    public VotingEligibilityService.EligibilityResult checkEligibility(Long pollId, Long flatId, Long userId) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found: " + pollId));
        if (eligibilityService == null) {
            return VotingEligibilityService.EligibilityResult.builder()
                    .eligible(true)
                    .reasonCode("ELIGIBLE")
                    .message("Eligible")
                    .flatId(flatId)
                    .weightApplied(BigDecimal.ONE)
                    .votingMode(poll.getVotingMode())
                    .build();
        }
        Long communityId = poll.getCommunity() != null ? poll.getCommunity().getId() : null;
        return eligibilityService.evaluate(
                communityId,
                flatId,
                userId,
                poll.getVotingMode(),
                poll.getExcludeDefaultersOverDays() != null ? poll.getExcludeDefaultersOverDays() : 90,
                poll.isOwnersOnly(),
                LocalDate.now());
    }

    @Transactional
    public PollResponse voteForFlat(Long pollId, Long flatId, List<Long> optionIds, AppUser voter) {
        if (flatId == null) {
            return vote(pollId, optionIds, voter);
        }
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found: " + pollId));
        if (poll.getClosesOn() != null && poll.getClosesOn().isBefore(LocalDate.now())) {
            throw new IllegalStateException("This poll has closed");
        }
        if (voteRepo.existsByPollIdAndFlatId(pollId, flatId)) {
            throw new IllegalStateException("One-flat-one-vote rule: a vote has already been cast for this flat");
        }
        if (!poll.isAllowMultiple() && optionIds.size() > 1) {
            throw new IllegalStateException("This poll allows only one selection");
        }

        VotingEligibilityService.EligibilityResult eligibility = checkEligibility(pollId, flatId, voter.getId());
        if (!eligibility.eligible()) {
            throw new IllegalStateException(eligibility.message());
        }

        String snapshot = eligibility.votingMode() + "|weight=" + eligibility.weightApplied() + "|" + eligibility.reasonCode();
        for (Long optionId : optionIds) {
            PollOption option = poll.getOptions().stream()
                    .filter(o -> o.getId().equals(optionId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid option: " + optionId));
            voteRepo.save(PollVote.builder()
                    .poll(poll)
                    .option(option)
                    .voter(voter)
                    .flatId(flatId)
                    .weightApplied(eligibility.weightApplied())
                    .eligibilitySnapshot(snapshot)
                    .build());
        }
        return toResponse(pollRepo.findById(pollId).orElseThrow(), voter.getId());
    }

    @Transactional
    public PollResponse closePoll(Long id, Long userId) {
        Poll poll = pollRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found: " + id));
        if (!poll.getCreatedBy().getId().equals(userId)) {
            throw new IllegalStateException("Only the creator can close this poll");
        }
        poll.setClosesOn(LocalDate.now().minusDays(1));
        return toResponse(pollRepo.save(poll), userId);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Poll poll = pollRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found: " + id));
        if (!poll.getCreatedBy().getId().equals(userId)) {
            throw new IllegalStateException("Only the creator can delete this poll");
        }
        voteRepo.deleteAll(voteRepo.findByPollId(id));
        pollRepo.delete(poll);
    }

    private PollResponse toResponse(Poll p, Long currentUserId) {
        List<PollVote> allVotes = voteRepo.findByPollId(p.getId());
        List<PollVote> myVotes = allVotes.stream()
                .filter(v -> v.getVoter().getId().equals(currentUserId)).toList();

        boolean isClosed = p.getClosesOn() != null && p.getClosesOn().isBefore(LocalDate.now());
        boolean hasVoted = !myVotes.isEmpty();
        boolean showResults = hasVoted || isClosed || p.getCreatedBy().getId().equals(currentUserId);

        double totalWeighted = showResults
                ? allVotes.stream().mapToDouble(v -> v.getWeightApplied() != null ? v.getWeightApplied().doubleValue() : 1.0).sum()
                : 0.0;

        return PollResponse.builder()
                .id(p.getId())
                .question(p.getQuestion())
                .description(p.getDescription())
                .closesOn(p.getClosesOn() != null ? p.getClosesOn().toString() : null)
                .allowMultiple(p.isAllowMultiple())
                .anonymous(p.isAnonymous())
                .votingMode(p.getVotingMode())
                .excludeDefaultersOverDays(p.getExcludeDefaultersOverDays())
                .ownersOnly(p.isOwnersOnly())
                .createdById(p.getCreatedBy().getId())
                .createdByName(p.getCreatedBy().getFullName())
                .communityId(p.getCommunity() != null ? p.getCommunity().getId() : null)
                .createdAt(formatDt(p.getCreatedAt()))
                .closed(isClosed)
                .hasVoted(hasVoted)
                .totalVotes((int) allVotes.stream().map(v -> v.getVoter().getId()).distinct().count())
                .totalWeightedVotes(Math.round(totalWeighted * 100.0) / 100.0)
                .options(p.getOptions().stream().map(o -> PollResponse.OptionDto.builder()
                        .id(o.getId())
                        .text(o.getText())
                        .sortOrder(o.getSortOrder())
                        .voteCount(showResults ? allVotes.stream().filter(v -> v.getOption().getId().equals(o.getId())).count() : 0)
                        .weightedScore(showResults ? Math.round(allVotes.stream()
                                .filter(v -> v.getOption().getId().equals(o.getId()))
                                .mapToDouble(v -> v.getWeightApplied() != null ? v.getWeightApplied().doubleValue() : 1.0)
                                .sum() * 100.0) / 100.0 : 0.0)
                        .selected(myVotes.stream().anyMatch(v -> v.getOption().getId().equals(o.getId())))
                        .build()).toList())
                .build();
    }

    private String formatDt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }
}
