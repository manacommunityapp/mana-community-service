package com.manacommunity.api.unit.service;

import com.manacommunity.api.governance.entity.*;
import com.manacommunity.api.governance.repository.*;
import com.manacommunity.api.governance.service.AgmQuorumService;
import com.manacommunity.api.governance.service.SecretBallotService;
import com.manacommunity.api.governance.service.VotingEligibilityService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Invoice;
import com.manacommunity.api.polling.engine.PollQuorumEngine;
import com.manacommunity.api.polling.entity.Poll;
import com.manacommunity.api.polling.entity.PollOption;
import com.manacommunity.api.polling.entity.PollVote;
import com.manacommunity.api.polling.repository.PollRepository;
import com.manacommunity.api.polling.repository.PollVoteRepository;
import com.manacommunity.api.polling.service.PollService;
import com.manacommunity.api.repository.InvoiceRepository;
import com.manacommunity.api.resident.model.CommunityFlat;
import com.manacommunity.api.resident.model.FlatMembership;
import com.manacommunity.api.resident.repository.CommunityFlatRepository;
import com.manacommunity.api.resident.repository.FlatMembershipRepository;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GovernanceSecretBallotAndAgmQuorumTest {

    @Mock private CommunityFlatRepository flatRepo;
    @Mock private FlatMembershipRepository membershipRepo;
    @Mock private InvoiceRepository invoiceRepo;
    @Mock private PollRepository pollRepo;
    @Mock private PollVoteRepository pollVoteRepo;

    @Mock private CommitteeElectionRepository electionRepo;
    @Mock private ElectionNominationRepository nominationRepo;
    @Mock private ElectionVoterRollRepository voterRollRepo;
    @Mock private ElectionSecretBallotRepository secretBallotRepo;

    @Mock private GovernanceMeetingRepository meetingRepo;
    @Mock private GovernanceResolutionRepository resolutionRepo;
    @Mock private AgmAttendanceLedgerRepository ledgerRepo;

    private VotingEligibilityService eligibilityService;
    private PollService pollService;
    private SecretBallotService secretBallotService;
    private AgmQuorumService agmQuorumService;

    @BeforeEach
    void setUp() {
        eligibilityService = new VotingEligibilityService(flatRepo, membershipRepo, invoiceRepo);
        pollService = new PollService(pollRepo, pollVoteRepo);
        pollService.setEligibilityService(eligibilityService);
        secretBallotService = new SecretBallotService(
                electionRepo, nominationRepo, voterRollRepo, secretBallotRepo, eligibilityService);
        agmQuorumService = new AgmQuorumService(meetingRepo, resolutionRepo, ledgerRepo);
    }

    @Test
    @DisplayName("Blocks 90-day maintenance defaulters while applying UDS weight and one-flat-one-vote cleanly")
    void shouldEnforce90DayDefaulterBlockAndFlatUdsWeight() {
        Community community = Community.builder().id(10L).name("Prestige Palms").build();
        CommunityFlat flat = CommunityFlat.builder()
                .id(100L).community(community).towerBlock("A").flatNumber("402")
                .udsSqft(new BigDecimal("1850.50")).shareUnits(2).build();
        FlatMembership ownerMembership = FlatMembership.builder()
                .flat(flat).residentType("OWNER").appAccessStatus("FULL_ACCESS").build();

        AppUser user = new AppUser();
        user.setId(50L);
        user.setFullName("Ananya Sharma");

        when(flatRepo.findById(100L)).thenReturn(Optional.of(flat));
        when(membershipRepo.findByUserIdAndFlatId(50L, 100L)).thenReturn(Optional.of(ownerMembership));

        // Scenario 1: Overdue > 90 days -> blocked
        Invoice overdue95Days = Invoice.builder()
                .id(1L).community(community).status("OVERDUE")
                .totalAmount(new BigDecimal("6500.00"))
                .dueDate(LocalDate.now().minusDays(95))
                .build();
        when(invoiceRepo.findByResidentIdOrderByGeneratedAtDesc(50L)).thenReturn(List.of(overdue95Days));

        var denied = eligibilityService.evaluate(10L, 100L, 50L,
                VotingEligibilityService.MODE_UDS_AREA_WEIGHTED, 90, true, LocalDate.now());
        assertThat(denied.eligible()).isFalse();
        assertThat(denied.reasonCode()).isEqualTo("DEFAULTED_OVER_90_DAYS");
        assertThat(denied.overdueAmount()).isEqualByComparingTo("6500.00");

        // Scenario 2: Invoice paid / recent (<90 days) -> eligible with exact UDS weight
        Invoice recentInvoice = Invoice.builder()
                .id(2L).community(community).status("UNPAID")
                .totalAmount(new BigDecimal("4000.00"))
                .dueDate(LocalDate.now().minusDays(30))
                .build();
        when(invoiceRepo.findByResidentIdOrderByGeneratedAtDesc(50L)).thenReturn(List.of(recentInvoice));

        PollOption yesOpt = PollOption.builder().id(700L).text("Approve").sortOrder(0).build();
        Poll poll = Poll.builder()
                .id(77L).question("Adopt FY27 Budget").community(community).createdBy(user)
                .votingMode(VotingEligibilityService.MODE_UDS_AREA_WEIGHTED)
                .excludeDefaultersOverDays(90).ownersOnly(true)
                .options(List.of(yesOpt))
                .build();
        when(pollRepo.findById(77L)).thenReturn(Optional.of(poll));
        when(pollVoteRepo.existsByPollIdAndFlatId(77L, 100L)).thenReturn(false);
        when(pollVoteRepo.save(any(PollVote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pollVoteRepo.findByPollId(77L)).thenReturn(List.of());

        pollService.voteForFlat(77L, 100L, List.of(700L), user);
        verify(pollVoteRepo).save(argThat(v ->
                v.getFlatId().equals(100L)
                        && v.getWeightApplied().compareTo(new BigDecimal("1850.50")) == 0));

        // Scenario 3: Co-owner attempting second vote on same flat fails one-flat-one-vote check
        when(pollVoteRepo.existsByPollIdAndFlatId(77L, 100L)).thenReturn(true);
        assertThatThrownBy(() -> pollService.voteForFlat(77L, 100L, List.of(700L), user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("One-flat-one-vote");

        // BigDecimal tally precision check
        PollQuorumEngine engine = new PollQuorumEngine();
        var tallies = engine.calculateWeightedTallyDecimal(List.of(
                new PollQuorumEngine.WeightedVote(700L, 1850.50),
                new PollQuorumEngine.WeightedVote(700L, 1249.55)
        ));
        assertThat(tallies.get(700L)).isEqualByComparingTo("3100.05");
    }

    @Test
    @DisplayName("Chaumian blind-signature secret ballot prevents voter linkage, blocks duplicates, and passes audit")
    void shouldExecuteChaumianBlindSignatureSecretBallot() {
        Community community = Community.builder().id(10L).name("Prestige Palms").build();
        CommunityFlat flat = CommunityFlat.builder().id(100L).community(community).towerBlock("A").flatNumber("402").build();
        FlatMembership membership = FlatMembership.builder().flat(flat).residentType("OWNER").appAccessStatus("FULL_ACCESS").build();

        CommitteeElection election = CommitteeElection.builder()
                .id(901L).communityId(10L).status("VOTING_OPEN").build();
        ElectionNomination nominee = ElectionNomination.builder().id(301L).electionId(901L).nomineeId(88L).build();

        List<ElectionVoterRoll> voterRolls = new ArrayList<>();
        List<ElectionSecretBallot> ballotBox = new ArrayList<>();

        when(electionRepo.findById(901L)).thenReturn(Optional.of(election));
        when(electionRepo.save(any(CommitteeElection.class))).thenAnswer(inv -> inv.getArgument(0));
        when(nominationRepo.findByElectionId(901L)).thenReturn(List.of(nominee));
        when(flatRepo.findById(100L)).thenReturn(Optional.of(flat));
        when(membershipRepo.findByUserIdAndFlatId(50L, 100L)).thenReturn(Optional.of(membership));
        when(invoiceRepo.findByResidentIdOrderByGeneratedAtDesc(50L)).thenReturn(List.of());

        when(voterRollRepo.findByElectionIdAndFlatId(eq(901L), eq(100L)))
                .thenAnswer(inv -> voterRolls.stream().findFirst());
        when(voterRollRepo.save(any(ElectionVoterRoll.class))).thenAnswer(inv -> {
            ElectionVoterRoll r = inv.getArgument(0);
            voterRolls.add(r);
            return r;
        });
        when(voterRollRepo.countByElectionId(901L)).thenAnswer(inv -> (long) voterRolls.size());

        when(secretBallotRepo.findByBallotNullifier(anyString())).thenAnswer(inv -> {
            String nul = inv.getArgument(0);
            return ballotBox.stream().filter(b -> b.getBallotNullifier().equals(nul)).findFirst();
        });
        when(secretBallotRepo.save(any(ElectionSecretBallot.class))).thenAnswer(inv -> {
            ElectionSecretBallot b = inv.getArgument(0);
            ballotBox.add(b);
            return b;
        });
        when(secretBallotRepo.findByElectionIdOrderByIdAsc(901L)).thenAnswer(inv -> ballotBox);
        when(secretBallotRepo.findByElectionIdAndReceiptCode(eq(901L), anyString())).thenAnswer(inv -> {
            String code = inv.getArgument(1);
            return ballotBox.stream().filter(b -> b.getReceiptCode().equals(code)).findFirst();
        });
        when(secretBallotRepo.countSecretVotesByNomination(901L))
                .thenAnswer(inv -> ballotBox.isEmpty()
                        ? Collections.emptyList()
                        : Collections.singletonList(new Object[]{301L, (long) ballotBox.size()}));

        // 1. Resident fetches Election public key & blinds random token locally
        Map<String, Object> pubKey = secretBallotService.getElectionPublicKey(901L);
        BigInteger n = new BigInteger((String) pubKey.get("modulusHex"), 16);
        BigInteger e = new BigInteger((String) pubKey.get("publicExponentHex"), 16);

        byte[] randBytes = new byte[32];
        new SecureRandom().nextBytes(randBytes);
        String tokenHex = HexFormat.of().formatHex(randBytes);
        BigInteger m = SecretBallotService.tokenToMessageInt(tokenHex, n);

        BigInteger r = BigInteger.valueOf(65537L).add(BigInteger.valueOf(123456789L));
        while (!r.gcd(n).equals(BigInteger.ONE)) {
            r = r.add(BigInteger.ONE);
        }
        BigInteger blindedM = m.multiply(r.modPow(e, n)).mod(n);

        // 2. Claim blind signature once (flat 100)
        Map<String, Object> signed = secretBallotService.issueBlindToken(901L, 100L, 50L, blindedM.toString(16));
        BigInteger sPrime = new BigInteger((String) signed.get("blindSignatureHex"), 16);

        // Duplicate claim for same flat fails
        assertThatThrownBy(() -> secretBallotService.issueBlindToken(901L, 100L, 50L, blindedM.toString(16)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already claimed");

        // 3. Unblind locally and submit anonymous ballot (no flatId/userId!)
        BigInteger unblindedS = sPrime.multiply(r.modInverse(n)).mod(n);
        Map<String, Object> castRes = secretBallotService.castSecretBallot(901L, 301L, tokenHex, unblindedS.toString(16));
        assertThat(castRes.get("accepted")).isEqualTo(true);
        String receipt = (String) castRes.get("receiptCode");
        assertThat(receipt).startsWith("SB-");

        // Duplicate nullifier rejected
        assertThatThrownBy(() -> secretBallotService.castSecretBallot(901L, 301L, tokenHex, unblindedS.toString(16)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate secret ballot");

        // 4. Election Officer verifies hash chain + zero ballot stuffing + receipt inclusion
        Map<String, Object> audit = secretBallotService.verifyBallotAuditChain(901L, receipt);
        assertThat(audit.get("chainValid")).isEqualTo(true);
        assertThat(audit.get("zeroStuffingVerified")).isEqualTo(true);
        assertThat(audit.get("receiptIncluded")).isEqualTo(true);
        assertThat(audit.get("tokensIssued")).isEqualTo(1L);
        assertThat(audit.get("ballotsCast")).isEqualTo(1);
    }

    @Test
    @DisplayName("Tracks hybrid AGM quorum, auto-revokes proxy on owner check-in without double count, and gates resolutions")
    void shouldEnforceLiveAgmQuorumAndProxyReplacement() {
        GovernanceMeeting meeting = GovernanceMeeting.builder()
                .id(801L).communityId(10L).type("AGM").title("Annual General Meeting 2026")
                .totalEligibleUnits(20)
                .requiredQuorumPct(new BigDecimal("25.00"))
                .quorumConfirmed(0).quorumMet(false)
                .build();
        GovernanceResolution budgetRes = GovernanceResolution.builder()
                .id(601L).communityId(10L).resolutionNumber("AGM-2026-01")
                .title("Approve FY27 Maintenance Budget").votingStatus("DRAFT")
                .build();

        Map<Long, AgmAttendanceLedger> ledgerMap = new LinkedHashMap<>();

        when(meetingRepo.findById(801L)).thenReturn(Optional.of(meeting));
        when(meetingRepo.save(any(GovernanceMeeting.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resolutionRepo.findById(601L)).thenReturn(Optional.of(budgetRes));
        when(resolutionRepo.save(any(GovernanceResolution.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerRepo.findByMeetingIdAndFlatId(eq(801L), anyLong()))
                .thenAnswer(inv -> Optional.ofNullable(ledgerMap.get(inv.getArgument(1))));
        when(ledgerRepo.save(any(AgmAttendanceLedger.class))).thenAnswer(inv -> {
            AgmAttendanceLedger e = inv.getArgument(0);
            e.setId((long) (ledgerMap.size() + 1));
            ledgerMap.put(e.getFlatId(), e);
            return e;
        });
        when(ledgerRepo.countByMeetingIdAndStatus(801L, "CHECKED_IN"))
                .thenAnswer(inv -> ledgerMap.values().stream().filter(e -> "CHECKED_IN".equals(e.getStatus())).count());
        when(ledgerRepo.countByMeetingIdAndProxyHolderUserIdAndStatusIn(eq(801L), anyLong(), anyList()))
                .thenReturn(0L);

        // Register & check in proxy for flat 1 -> 1/20 = 5% (below 25% quorum)
        agmQuorumService.registerProxy(801L, 1L, 11L, 99L);
        Map<String, Object> proxyIn = agmQuorumService.checkIn(801L, 1L, 99L, "PROXY", "IN_PERSON_QR");
        assertThat(proxyIn.get("quorumConfirmed")).isEqualTo(1);
        assertThat(proxyIn.get("quorumMet")).isEqualTo(false);

        // Owner of flat 1 arrives online -> auto-supersedes proxy without increasing count (stays 1/20)
        Map<String, Object> ownerIn = agmQuorumService.checkIn(801L, 1L, 11L, "OWNER", "ONLINE_WS");
        assertThat(ownerIn.get("proxySuperseded")).isEqualTo(true);
        assertThat(ownerIn.get("quorumConfirmed")).isEqualTo(1);

        // Resolution cannot open below 25% quorum
        assertThatThrownBy(() -> agmQuorumService.openResolutionVoting(601L, 801L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("quorum not met");

        // Flats 2..5 check in (4 more units -> 5/20 = 25.00%) -> unlocks resolution voting
        for (long flatId = 2L; flatId <= 5L; flatId++) {
            agmQuorumService.checkIn(801L, flatId, 100L + flatId, "OWNER", "IN_PERSON_QR");
        }
        assertThat(meeting.isQuorumMet()).isTrue();
        GovernanceResolution opened = agmQuorumService.openResolutionVoting(601L, 801L);
        assertThat(opened.getVotingStatus()).isEqualTo("VOTING_OPEN");
    }
}
