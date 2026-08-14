package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class NoticeBoardPermissions {

    private NoticeBoardPermissions() {}

    public static final String VIEW_NOTICES  = "View Notices";
    public static final String CREATE_NOTICE = "Create Notice";
    public static final String DELETE_NOTICE = "Delete Notice";

    public static final List<String> ALL = List.of(
            VIEW_NOTICES, CREATE_NOTICE, DELETE_NOTICE
    );
}
