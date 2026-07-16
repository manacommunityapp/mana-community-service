package com.manacommunity.api.dto;

public record CommunityLeaderResponse(
        Long id,
        Long userId,
        String fullName,
        String profilePicUrl,
        String designation,
        String committee,
        String contactPhone,
        String contactEmail,
        String flatNo,
        String block,
        Integer displayOrder
) {}
