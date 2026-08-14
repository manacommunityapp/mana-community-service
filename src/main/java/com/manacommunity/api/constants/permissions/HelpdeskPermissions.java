package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class HelpdeskPermissions {

    private HelpdeskPermissions() {}

    public static final String VIEW_TICKETS   = "View Tickets";
    public static final String CREATE_TICKET  = "Create Ticket";
    public static final String MANAGE_TICKETS = "Manage Tickets";

    public static final List<String> ALL = List.of(
            VIEW_TICKETS, CREATE_TICKET, MANAGE_TICKETS
    );
}
