package com.manacommunity.api.dto.chat;

import java.util.List;

public record CreateGroupRequest(String title, List<Long> participantUserIds) {
    public List<Long> memberUserIds() {
        return participantUserIds;
    }
}
