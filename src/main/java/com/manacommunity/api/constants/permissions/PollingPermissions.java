package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class PollingPermissions {

    private PollingPermissions() {}

    public static final String VIEW_POLLS  = "View Polls";
    public static final String CREATE_POLL = "Create Poll";
    public static final String VOTE_POLL   = "Vote Poll";

    public static final List<String> ALL = List.of(
            VIEW_POLLS, CREATE_POLL, VOTE_POLL
    );
}
