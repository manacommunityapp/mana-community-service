package com.manacommunity.api.sports.dto;

/** Minimal family choice data for registering a community resident in a sports event. */
public record SportsCommunityFamilyMemberResponse(
        Long id,
        String name,
        String relation,
        Integer age,
        String gender
) {}
