package com.manacommunity.api.communitypolls.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agm_voting_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgmVotingTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private VotingScenario scenario;

    @Enumerated(EnumType.STRING)
    private VotingModel votingModel;

    private int totalEligibleFlats;
    private int quorumThresholdPercent;
    private int currentVoteCount;
    private boolean quorumMet;
    private boolean pollOpened;

    private boolean defaulterBlocked;
    private double defaulterDuesAmount;
    private boolean secretBallotAnonymous;
    private boolean voteTraceableToFlat;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum VotingScenario {
        ONE_FLAT_ONE_VOTE_QUORUM_MET, UDS_WEIGHTED_VOTING, DEFAULTER_BLOCKED_FROM_VOTING,
        SECRET_BALLOT_ANONYMITY_CHECK, QUORUM_NOT_MET_POLL_BLOCKED, PROXY_VOTE_VALIDATION,
        DUPLICATE_VOTE_PREVENTION, LIVE_QUORUM_THRESHOLD_UPDATE
    }

    public enum VotingModel { ONE_FLAT_ONE_VOTE, UDS_WEIGHTED, PROXY }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
