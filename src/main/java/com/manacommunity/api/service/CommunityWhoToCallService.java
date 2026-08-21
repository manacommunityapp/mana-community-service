package com.manacommunity.api.service;

import com.manacommunity.api.dto.CommunityWhoToCallHistoryResponse;
import com.manacommunity.api.dto.CommunityWhoToCallRequest;
import com.manacommunity.api.dto.CommunityWhoToCallResponse;

import java.util.List;

public interface CommunityWhoToCallService {

    List<CommunityWhoToCallResponse> getActive(Long communityId);

    List<CommunityWhoToCallResponse> getAll(Long communityId);

    CommunityWhoToCallResponse getById(Long id);

    CommunityWhoToCallResponse create(Long communityId, Long userId, String userName, CommunityWhoToCallRequest req);

    CommunityWhoToCallResponse update(Long id, Long userId, String userName, CommunityWhoToCallRequest req);

    void toggleStatus(Long id, Long userId, String userName);

    void delete(Long id, Long userId, String userName);

    CommunityWhoToCallResponse restore(Long id, Long userId, String userName);

    List<CommunityWhoToCallHistoryResponse> getHistory(Long whoToCallId);

    List<CommunityWhoToCallHistoryResponse> getAllCommunityHistory(Long communityId);
}
