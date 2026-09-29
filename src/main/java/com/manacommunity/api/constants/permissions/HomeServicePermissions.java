package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class HomeServicePermissions {
    private HomeServicePermissions() {}

    public static final String VIEW_HOME_SERVICE   = "View Home Service";
    public static final String MANAGE_WORKER       = "Manage Worker";
    public static final String BOOK_SERVICE        = "Book Service";
    public static final String LOG_ATTENDANCE      = "Log Attendance";
    public static final String REVIEW_WORKER       = "Review Worker";
    public static final String ADMIN_HOME_SERVICE  = "Admin Home Service";

    public static final List<String> ALL = List.of(
            VIEW_HOME_SERVICE, MANAGE_WORKER, BOOK_SERVICE,
            LOG_ATTENDANCE, REVIEW_WORKER, ADMIN_HOME_SERVICE
    );
}
