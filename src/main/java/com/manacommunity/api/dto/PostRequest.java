package com.manacommunity.api.dto;

import com.manacommunity.api.model.PostType;

public record PostRequest(
    String content,
    String imageUrl,
    PostType type,
    Double price,
    String location,
    String pollQuestion,
    String pollOptions
) {}
