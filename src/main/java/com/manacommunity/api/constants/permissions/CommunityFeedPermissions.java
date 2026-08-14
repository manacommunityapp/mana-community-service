package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class CommunityFeedPermissions {

    private CommunityFeedPermissions() {}

    public static final String VIEW_FEED       = "View Feed";
    public static final String CREATE_POST     = "Create Post";
    public static final String DELETE_POST     = "Delete Post";
    public static final String COMMENT_ON_POST = "Comment on Post";

    public static final List<String> ALL = List.of(
            VIEW_FEED, CREATE_POST, DELETE_POST, COMMENT_ON_POST
    );
}
