package com.manacommunity.api.communitypolls.controller;

import com.manacommunity.api.communitypolls.entity.*;
import com.manacommunity.api.communitypolls.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community-polls-test")
public class CommunityPollsTestController {

    private final AgmVotingTestRepository agmRepo;
    private final DirectoryPrivacyTestRepository privacyRepo;
    private final ClassifiedsModerationTestRepository classifiedsRepo;

    public CommunityPollsTestController(AgmVotingTestRepository agmRepo,
                                         DirectoryPrivacyTestRepository privacyRepo,
                                         ClassifiedsModerationTestRepository classifiedsRepo) {
        this.agmRepo = agmRepo;
        this.privacyRepo = privacyRepo;
        this.classifiedsRepo = classifiedsRepo;
    }

    public record PollsDashboard(int totalTests, int passed, int failed, int pending, double passRate,
                                  List<AgmVotingTest> votingTests, List<DirectoryPrivacyTest> privacyTests,
                                  List<ClassifiedsModerationTest> classifiedsTests) {}

    @GetMapping("/dashboard")
    public ResponseEntity<PollsDashboard> getDashboard() {
        var v = agmRepo.findAll(); var p = privacyRepo.findAll(); var c = classifiedsRepo.findAll();
        int total = v.size() + p.size() + c.size();
        if (total == 0) return seedDemo();
        long pass = v.stream().filter(t -> t.getTestResult() == AgmVotingTest.TestResult.PASS).count()
                + p.stream().filter(t -> t.getTestResult() == DirectoryPrivacyTest.TestResult.PASS).count()
                + c.stream().filter(t -> t.getTestResult() == ClassifiedsModerationTest.TestResult.PASS).count();
        long fail = total - pass;
        return ResponseEntity.ok(new PollsDashboard(total, (int) pass, (int) fail, 0, Math.round(pass * 1000.0 / total) / 10.0, v, p, c));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<PollsDashboard> seedDemo() {
        var v1 = AgmVotingTest.builder().communityId("COMM-001").testCaseCode("AGM-001").testCaseName("One-flat-one-vote — quorum met at 51%")
                .scenario(AgmVotingTest.VotingScenario.ONE_FLAT_ONE_VOTE_QUORUM_MET).votingModel(AgmVotingTest.VotingModel.ONE_FLAT_ONE_VOTE)
                .totalEligibleFlats(200).quorumThresholdPercent(51).currentVoteCount(108).quorumMet(true).pollOpened(true)
                .defaulterBlocked(false).defaulterDuesAmount(0).secretBallotAnonymous(true).voteTraceableToFlat(false)
                .testResult(AgmVotingTest.TestResult.PASS).failureReason("").build();

        var v2 = AgmVotingTest.builder().communityId("COMM-001").testCaseCode("AGM-002").testCaseName("Defaulter ₹12,000 dues — allowed to vote")
                .scenario(AgmVotingTest.VotingScenario.DEFAULTER_BLOCKED_FROM_VOTING).votingModel(AgmVotingTest.VotingModel.ONE_FLAT_ONE_VOTE)
                .totalEligibleFlats(200).quorumThresholdPercent(51).currentVoteCount(102).quorumMet(true).pollOpened(true)
                .defaulterBlocked(false).defaulterDuesAmount(12000).secretBallotAnonymous(true).voteTraceableToFlat(false)
                .testResult(AgmVotingTest.TestResult.FAIL).failureReason("Defaulter with ₹12,000 pending was not blocked — dues check skipped for proxy votes").build();

        var v3 = AgmVotingTest.builder().communityId("COMM-001").testCaseCode("AGM-003").testCaseName("Secret ballot — vote traceable to flat")
                .scenario(AgmVotingTest.VotingScenario.SECRET_BALLOT_ANONYMITY_CHECK).votingModel(AgmVotingTest.VotingModel.ONE_FLAT_ONE_VOTE)
                .totalEligibleFlats(200).quorumThresholdPercent(51).currentVoteCount(115).quorumMet(true).pollOpened(true)
                .defaulterBlocked(false).defaulterDuesAmount(0).secretBallotAnonymous(false).voteTraceableToFlat(true)
                .testResult(AgmVotingTest.TestResult.FAIL).failureReason("Audit log includes flat_id with vote_choice — anonymity violated").build();

        var p1 = DirectoryPrivacyTest.builder().communityId("COMM-001").testCaseCode("PRV-001").testCaseName("Phone hidden from neighbors — search API")
                .scenario(DirectoryPrivacyTest.PrivacyScenario.PHONE_HIDDEN_FROM_NEIGHBORS).fieldName("phone_number")
                .configuredVisibility(DirectoryPrivacyTest.VisibilityLevel.MC_ONLY)
                .visibleInNeighborSearch(false).visibleInMcDashboard(true).visibleInClubDirectory(false)
                .leakedInJsonResponse(false).leakedEndpoint("")
                .testResult(DirectoryPrivacyTest.TestResult.PASS).failureReason("").build();

        var p2 = DirectoryPrivacyTest.builder().communityId("COMM-001").testCaseCode("PRV-002").testCaseName("Vehicle number leaked in search JSON")
                .scenario(DirectoryPrivacyTest.PrivacyScenario.FIELD_LEAKED_IN_SEARCH_API).fieldName("vehicle_number")
                .configuredVisibility(DirectoryPrivacyTest.VisibilityLevel.MC_ONLY)
                .visibleInNeighborSearch(false).visibleInMcDashboard(true).visibleInClubDirectory(false)
                .leakedInJsonResponse(true).leakedEndpoint("GET /api/residents/search?q=A-401")
                .testResult(DirectoryPrivacyTest.TestResult.FAIL).failureReason("vehicle_number present in search response body despite MC_ONLY visibility").build();

        var c1 = ClassifiedsModerationTest.builder().communityId("COMM-001").testCaseCode("CLS-001").testCaseName("Society-only classified — not visible outside")
                .scenario(ClassifiedsModerationTest.ClassifiedScenario.SOCIETY_ONLY_VISIBILITY).geoScope(ClassifiedsModerationTest.GeoScope.SOCIETY_ONLY)
                .postTitle("Selling sofa set — ₹8,000").imageUploaded(true).imageOriginalSizeBytes(4_200_000).imageCompressedSizeBytes(820_000).compressionRatio(80.5)
                .profanityDetected(false).detectedProfanity("").phishingLinkDetected(false).detectedLink("").postBlocked(false).autoModerated(false)
                .testResult(ClassifiedsModerationTest.TestResult.PASS).failureReason("").build();

        var c2 = ClassifiedsModerationTest.builder().communityId("COMM-001").testCaseCode("CLS-002").testCaseName("Phishing link in post body — not blocked")
                .scenario(ClassifiedsModerationTest.ClassifiedScenario.PHISHING_LINK_BLOCKED).geoScope(ClassifiedsModerationTest.GeoScope.SOCIETY_ONLY)
                .postTitle("Free WiFi router").imageUploaded(false).imageOriginalSizeBytes(0).imageCompressedSizeBytes(0).compressionRatio(0)
                .profanityDetected(false).detectedProfanity("").phishingLinkDetected(true).detectedLink("http://fr33-r0uter.xyz/claim")
                .postBlocked(false).autoModerated(false)
                .testResult(ClassifiedsModerationTest.TestResult.FAIL).failureReason("Phishing link http://fr33-r0uter.xyz/claim not caught by URL filter").build();

        var c3 = ClassifiedsModerationTest.builder().communityId("COMM-001").testCaseCode("CLS-003").testCaseName("Image compression 4.2MB → 820KB")
                .scenario(ClassifiedsModerationTest.ClassifiedScenario.IMAGE_COMPRESSION_CHECK).geoScope(ClassifiedsModerationTest.GeoScope.SOCIETY_ONLY)
                .postTitle("Used bicycle for kids").imageUploaded(true).imageOriginalSizeBytes(4_200_000).imageCompressedSizeBytes(820_000).compressionRatio(80.5)
                .profanityDetected(false).detectedProfanity("").phishingLinkDetected(false).detectedLink("").postBlocked(false).autoModerated(false)
                .testResult(ClassifiedsModerationTest.TestResult.PASS).failureReason("").build();

        agmRepo.saveAll(List.of(v1, v2, v3));
        privacyRepo.saveAll(List.of(p1, p2));
        classifiedsRepo.saveAll(List.of(c1, c2, c3));

        return ResponseEntity.ok(new PollsDashboard(8, 4, 4, 0, 50.0,
                List.of(v1, v2, v3), List.of(p1, p2), List.of(c1, c2, c3)));
    }
}
