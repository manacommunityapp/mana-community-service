package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class GuardPermissions {

    private GuardPermissions() {}

    public static final String VIEW_GUARDS   = "View Guards";
    public static final String MANAGE_GUARDS = "Manage Guards";

    public static final List<String> ALL = List.of(VIEW_GUARDS, MANAGE_GUARDS);
}
