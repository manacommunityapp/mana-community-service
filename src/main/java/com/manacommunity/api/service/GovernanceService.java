package com.manacommunity.api.service;

import com.manacommunity.api.dto.*;

import java.util.List;

public interface GovernanceService {
    GovernanceStatsResponse getStats(Long communityId, Long userId);
    List<MeetingResponse> getMeetings(Long communityId, Long userId, String type);
    MeetingResponse getMeetingById(Long communityId, Long userId, Long meetingId);
    void confirmAttendance(Long communityId, Long userId, Long meetingId, String mode, String proxyNominee);
    List<ProposalResponse> getProposals(Long communityId, Long userId, String category);
    ProposalResponse createProposal(Long communityId, Long userId, ProposalRequest request);
    ProposalSupportResponse supportProposal(Long communityId, Long userId, Long proposalId);
    List<BallotResponse> getBallots(Long communityId, Long userId, String status);
    BallotVoteResponse castVote(Long communityId, Long userId, Long ballotId, String optionId);
    List<ResolutionResponse> getResolutions(Long communityId, String search);
    List<VaultDocumentResponse> getVaultDocuments(Long communityId, String category, String search);
}
