package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class VisitorPermissions {

    private VisitorPermissions() {}

    public static final String VIEW_VISITORS      = "View Visitors";
    public static final String CREATE_VISITOR_PASS = "Create Visitor Pass";
    public static final String MANAGE_GATE        = "Manage Gate";

    public static final List<String> ALL = List.of(
            VIEW_VISITORS, CREATE_VISITOR_PASS, MANAGE_GATE
    );
}
