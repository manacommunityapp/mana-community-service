package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.service.GovernanceService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GovernanceServiceImpl implements GovernanceService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final CommunityMeetingRepository meetingRepo;
    private final MeetingRsvpRepository rsvpRepo;
    private final CommunityProposalRepository proposalRepo;
    private final ProposalSupportRepository supportRepo;
    private final CommunityBallotRepository ballotRepo;
    private final BallotVoteRepository voteRepo;
    private final CommunityResolutionRepository resolutionRepo;
    private final GovernanceVaultDocumentRepository vaultRepo;
    private final AppUserRepository userRepo;

    @Override
    public GovernanceStatsResponse getStats(Long communityId, Long userId) {
        long upcoming = meetingRepo.countByCommunityIdAndStatus(communityId, "UPCOMING");
        long active   = proposalRepo.countByCommunityIdAndStatusIn(communityId, List.of("SUBMITTED", "UNDER_REVIEW"));
        long openVotes = ballotRepo.countByCommunityIdAndStatus(communityId, "ACTIVE");
        long passed   = resolutionRepo.countByCommunityIdAndStatus(communityId, "PASSED")
                       + resolutionRepo.countByCommunityIdAndStatus(communityId, "IN_IMPLEMENTATION")
                       + resolutionRepo.countByCommunityIdAndStatus(communityId, "COMPLETED");
        return GovernanceStatsResponse.builder()
                .upcomingMeetings(upcoming)
                .activeProposals(active)
                .openVotes(openVotes)
                .passedResolutions(passed)
                .actionItemsOpen(0)
                .myMeetingsAttended(0)
                .myVotesRecorded(0)
                .communityQuorumRate(0)
                .build();
    }

    @Override
    public List<MeetingResponse> getMeetings(Long communityId, Long userId, String type) {
        List<CommunityMeeting> meetings;
        if (type != null && !type.isBlank()) {
            meetings = meetingRepo.findByCommunityIdAndTypeOrderByMeetingDateDesc(communityId, type);
        } else {
            meetings = meetingRepo.findByCommunityIdOrderByMeetingDateDesc(communityId);
        }
        return meetings.stream().map(m -> toMeetingResponse(m, userId)).collect(Collectors.toList());
    }

    @Override
    public MeetingResponse getMeetingById(Long communityId, Long userId, Long meetingId) {
        CommunityMeeting meeting = meetingRepo.findById(meetingId)
                .filter(m -> communityId.equals(m.getCommunityId()))
                .orElseThrow(() -> new NoSuchElementException("Meeting not found"));
        return toMeetingResponse(meeting, userId);
    }

    @Override
    @Transactional
    public void confirmAttendance(Long communityId, Long userId, Long meetingId, String mode, String proxyNominee) {
        CommunityMeeting meeting = meetingRepo.findById(meetingId)
                .filter(m -> communityId.equals(m.getCommunityId()))
                .orElseThrow(() -> new NoSuchElementException("Meeting not found"));

        MeetingRsvp rsvp = rsvpRepo.findByMeetingIdAndUserId(meetingId, userId)
                .orElseGet(() -> MeetingRsvp.builder().meeting(meeting).userId(userId).build());
        rsvp.setMode(mode);
        rsvp.setProxyNominee(proxyNominee);
        rsvpRepo.save(rsvp);
    }

    @Override
    public List<ProposalResponse> getProposals(Long communityId, Long userId, String category) {
        List<CommunityProposal> proposals;
        if (category != null && !category.isBlank()) {
            proposals = proposalRepo.findByCommunityIdAndCategoryOrderByCreatedAtDesc(communityId, category);
        } else {
            proposals = proposalRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        }
        return proposals.stream().map(p -> toProposalResponse(p, userId)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProposalResponse createProposal(Long communityId, Long userId, ProposalRequest req) {
        AppUser user = userRepo.findById(userId).orElseThrow();
        CommunityProposal proposal = CommunityProposal.builder()
                .communityId(communityId)
                .submittedByUserId(userId)
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory() != null ? req.getCategory() : "AMENITIES")
                .type(req.getType() != null ? req.getType() : "PROPOSAL")
                .estimatedCost(req.getEstimatedCost())
                .pros(req.getPros() != null ? req.getPros() : new ArrayList<>())
                .cons(req.getCons() != null ? req.getCons() : new ArrayList<>())
                .build();

        // auto-support by submitter
        ProposalSupport ownSupport = ProposalSupport.builder()
                .proposal(proposal)
                .userId(userId)
                .build();
        proposal.getSupports().add(ownSupport);

        CommunityProposal saved = proposalRepo.save(proposal);
        return toProposalResponse(saved, userId);
    }

    @Override
    @Transactional
    public ProposalSupportResponse supportProposal(Long communityId, Long userId, Long proposalId) {
        CommunityProposal proposal = proposalRepo.findById(proposalId)
                .filter(p -> communityId.equals(p.getCommunityId()))
                .orElseThrow(() -> new NoSuchElementException("Proposal not found"));

        Optional<ProposalSupport> existing = supportRepo.findByProposalIdAndUserId(proposalId, userId);
        boolean nowSupported;
        if (existing.isPresent()) {
            supportRepo.delete(existing.get());
            nowSupported = false;
        } else {
            ProposalSupport support = ProposalSupport.builder()
                    .proposal(proposal)
                    .userId(userId)
                    .build();
            supportRepo.save(support);
            nowSupported = true;
        }

        long count = supportRepo.countByProposalId(proposalId);
        if (count >= proposal.getSupportThreshold() && !"APPROVED_FOR_AGM".equals(proposal.getStatus())
                && !"PASSED".equals(proposal.getStatus())) {
            proposal.setStatus("APPROVED_FOR_AGM");
            proposalRepo.save(proposal);
        }

        return ProposalSupportResponse.builder()
                .success(true)
                .supportCount((int) count)
                .hasUserSupported(nowSupported)
                .build();
    }

    @Override
    public List<BallotResponse> getBallots(Long communityId, Long userId, String status) {
        List<CommunityBallot> ballots;
        if (status != null && !status.isBlank()) {
            ballots = ballotRepo.findByCommunityIdAndStatusOrderByStartDateDesc(communityId, status);
        } else {
            ballots = ballotRepo.findByCommunityIdOrderByStartDateDesc(communityId);
        }
        return ballots.stream().map(b -> toBallotResponse(b, userId)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BallotVoteResponse castVote(Long communityId, Long userId, Long ballotId, String optionIdStr) {
        CommunityBallot ballot = ballotRepo.findById(ballotId)
                .filter(b -> communityId.equals(b.getCommunityId()))
                .orElseThrow(() -> new NoSuchElementException("Ballot not found"));

        if (!"ACTIVE".equals(ballot.getStatus())) {
            throw new IllegalStateException("Ballot is not active");
        }

        if (voteRepo.findByBallotIdAndUserId(ballotId, userId).isPresent()) {
            throw new IllegalStateException("Already voted");
        }

        Long optionId = Long.parseLong(optionIdStr);
        BallotOption option = ballot.getOptions().stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Option not found"));

        String receiptHash = "0x" + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        BallotVote vote = BallotVote.builder()
                .ballot(ballot)
                .option(option)
                .userId(userId)
                .receiptHash(receiptHash)
                .build();
        voteRepo.save(vote);

        return BallotVoteResponse.builder().receiptHash(receiptHash).build();
    }

    @Override
    public List<ResolutionResponse> getResolutions(Long communityId, String search) {
        List<CommunityResolution> resolutions;
        if (search != null && !search.isBlank()) {
            resolutions = resolutionRepo.findByCommunityIdAndTitleContainingIgnoreCaseOrderByPassedDateDesc(communityId, search);
        } else {
            resolutions = resolutionRepo.findByCommunityIdOrderByPassedDateDesc(communityId);
        }
        return resolutions.stream().map(this::toResolutionResponse).collect(Collectors.toList());
    }

    @Override
    public List<VaultDocumentResponse> getVaultDocuments(Long communityId, String category, String search) {
        List<GovernanceVaultDocument> docs;
        if (search != null && !search.isBlank()) {
            docs = vaultRepo.findByCommunityIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(communityId, search);
        } else if (category != null && !category.isBlank()) {
            docs = vaultRepo.findByCommunityIdAndCategoryOrderByCreatedAtDesc(communityId, category);
        } else {
            docs = vaultRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        }
        return docs.stream().map(this::toVaultDocResponse).collect(Collectors.toList());
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private MeetingResponse toMeetingResponse(CommunityMeeting m, Long userId) {
        long confirmed = rsvpRepo.countByMeetingId(m.getId());
        Optional<MeetingRsvp> userRsvp = rsvpRepo.findByMeetingIdAndUserId(m.getId(), userId);
        double pct = m.getQuorumRequired() > 0
                ? Math.min(100.0, (confirmed * 100.0) / m.getQuorumRequired())
                : 0;

        List<MeetingResponse.AgendaItemResponse> agenda = m.getAgendaItems().stream()
                .sorted(Comparator.comparingInt(MeetingAgendaItem::getItemOrder))
                .map(a -> MeetingResponse.AgendaItemResponse.builder()
                        .id(a.getId())
                        .order(a.getItemOrder())
                        .title(a.getTitle())
                        .presenter(a.getPresenter())
                        .duration(a.getDuration())
                        .description(a.getDescription())
                        .status(a.getStatus())
                        .notes(a.getNotes())
                        .attachmentName(a.getAttachmentName())
                        .build())
                .collect(Collectors.toList());

        return MeetingResponse.builder()
                .id(m.getId().toString())
                .type(m.getType())
                .title(m.getTitle())
                .description(m.getDescription())
                .date(m.getMeetingDate() != null ? m.getMeetingDate().format(DATE_FMT) : null)
                .time(m.getMeetingTime() != null ? m.getMeetingTime().format(TIME_FMT) : null)
                .location(m.getLocation())
                .meetingLink(m.getMeetingLink())
                .status(m.getStatus())
                .quorumConfirmed((int) confirmed)
                .quorumRequired(m.getQuorumRequired())
                .quorumPercentage(Math.round(pct * 10.0) / 10.0)
                .isQuorumAchieved(confirmed >= m.getQuorumRequired())
                .hasUserConfirmed(userRsvp.isPresent())
                .attendanceMode(userRsvp.map(MeetingRsvp::getMode).orElse("PHYSICAL"))
                .proxyNominee(userRsvp.map(MeetingRsvp::getProxyNominee).orElse(null))
                .annualReportUrl(m.getAnnualReportUrl())
                .minutesUrl(m.getMinutesUrl())
                .agenda(agenda)
                .build();
    }

    private ProposalResponse toProposalResponse(CommunityProposal p, Long userId) {
        long count = supportRepo.countByProposalId(p.getId());
        boolean userSupported = supportRepo.findByProposalIdAndUserId(p.getId(), userId).isPresent();
        AppUser submitter = userRepo.findById(p.getSubmittedByUserId()).orElse(null);

        return ProposalResponse.builder()
                .id(p.getId().toString())
                .title(p.getTitle())
                .description(p.getDescription())
                .category(p.getCategory())
                .type(p.getType())
                .estimatedCost(p.getEstimatedCost())
                .submittedBy(submitter != null ? submitter.getFullName() : "Resident")
                .submittedByFlat(submitter != null ? submitter.getFlatNo() : null)
                .submittedDate(p.getCreatedAt() != null ? p.getCreatedAt().format(DATE_FMT) : "Unknown")
                .supportCount((int) count)
                .supportThreshold(p.getSupportThreshold())
                .hasUserSupported(userSupported)
                .status(p.getStatus())
                .pros(p.getPros())
                .cons(p.getCons())
                .committeeNotes(p.getCommitteeNotes())
                .build();
    }

    private BallotResponse toBallotResponse(CommunityBallot b, Long userId) {
        long totalVotes = voteRepo.countByBallotId(b.getId());
        Optional<BallotVote> userVote = voteRepo.findByBallotIdAndUserId(b.getId(), userId);
        double turnout = b.getTotalEligibleVoters() > 0
                ? Math.min(100.0, (totalVotes * 100.0) / b.getTotalEligibleVoters())
                : 0;

        List<BallotResponse.BallotOptionResponse> options = b.getOptions().stream().map(o -> {
            long optVotes = voteRepo.countByOptionId(o.getId());
            double pct = totalVotes > 0 ? Math.round((optVotes * 100.0 / totalVotes) * 10.0) / 10.0 : 0;
            return BallotResponse.BallotOptionResponse.builder()
                    .id(o.getId().toString())
                    .text(o.getText())
                    .description(o.getDescription())
                    .votesCount(optVotes)
                    .percentage(pct)
                    .build();
        }).collect(Collectors.toList());

        return BallotResponse.builder()
                .id(b.getId().toString())
                .title(b.getTitle())
                .category(b.getCategory())
                .description(b.getDescription())
                .resolutionMotionText(b.getResolutionMotionText())
                .startDate(b.getStartDate() != null ? b.getStartDate().format(DT_FMT) : null)
                .endDate(b.getEndDate() != null ? b.getEndDate().format(DT_FMT) : null)
                .totalEligibleVoters(b.getTotalEligibleVoters())
                .totalVotesCast(totalVotes)
                .turnoutPercentage(Math.round(turnout * 10.0) / 10.0)
                .minQuorumPercentage(b.getMinQuorumPercentage())
                .isQuorumReached(turnout >= b.getMinQuorumPercentage())
                .hasUserVoted(userVote.isPresent())
                .userVotedOptionId(userVote.map(v -> v.getOption().getId().toString()).orElse(null))
                .voteReceiptHash(userVote.map(BallotVote::getReceiptHash).orElse(null))
                .status(b.getStatus())
                .options(options)
                .build();
    }

    private ResolutionResponse toResolutionResponse(CommunityResolution r) {
        int total = r.getForVotes() + r.getAgainstVotes() + r.getAbstainedVotes();
        double pctFor = total > 0 ? Math.round((r.getForVotes() * 100.0 / total) * 10.0) / 10.0 : 0;

        return ResolutionResponse.builder()
                .id(r.getId().toString())
                .resolutionNumber(r.getResolutionNumber())
                .title(r.getTitle())
                .meetingReference(r.getMeetingReference())
                .passedDate(r.getPassedDate() != null ? r.getPassedDate().format(DATE_FMT) : null)
                .category(r.getCategory())
                .approvedBudget(r.getApprovedBudget())
                .status(r.getStatus())
                .votingSummary(ResolutionResponse.VotingSummary.builder()
                        .forVotes(r.getForVotes())
                        .againstVotes(r.getAgainstVotes())
                        .abstained(r.getAbstainedVotes())
                        .totalPercentageFor(pctFor)
                        .build())
                .keyProvisions(r.getKeyProvisions())
                .presidingOfficer(r.getPresidingOfficer())
                .legalBindingStatement(r.getLegalBindingStatement())
                .documentUrl(r.getDocumentUrl())
                .build();
    }

    private VaultDocumentResponse toVaultDocResponse(GovernanceVaultDocument d) {
        return VaultDocumentResponse.builder()
                .id(d.getId().toString())
                .title(d.getTitle())
                .category(d.getCategory())
                .fileSize(d.getFileSize())
                .fileFormat(d.getFileFormat())
                .lastUpdated(d.getUpdatedAt() != null ? d.getUpdatedAt().format(DATE_FMT) : null)
                .documentUrl(d.getDocumentUrl())
                .isConfidential(Boolean.TRUE.equals(d.getIsConfidential()))
                .resolutionRef(d.getResolutionRef())
                .digitalSealVerified(Boolean.TRUE.equals(d.getDigitalSealVerified()))
                .build();
    }
}
