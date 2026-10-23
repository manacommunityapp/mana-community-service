package com.manacommunity.api.dto.chat;

import java.util.List;

/** Request payload for adding or removing members from a group conversation */
public record GroupMemberRequest(List<Long> userIds) {}
