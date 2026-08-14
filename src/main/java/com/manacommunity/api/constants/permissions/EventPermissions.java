package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class EventPermissions {

    private EventPermissions() {}

    public static final String VIEW_EVENTS    = "View Events";
    public static final String CREATE_EVENT   = "Create Event";
    public static final String REGISTER_EVENT = "Register Event";

    public static final List<String> ALL = List.of(
            VIEW_EVENTS, CREATE_EVENT, REGISTER_EVENT
    );
}
