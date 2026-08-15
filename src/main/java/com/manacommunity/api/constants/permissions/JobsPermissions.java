package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class JobsPermissions {

    private JobsPermissions() {}

    public static final String VIEW_JOBS  = "View Jobs";
    public static final String CREATE_JOB = "Create Job";
    public static final String APPLY_JOB  = "Apply Job";

    public static final List<String> ALL = List.of(
            VIEW_JOBS, CREATE_JOB, APPLY_JOB
    );
}
