package com.manacommunity.api.constants.permissions;

import java.util.List;

public final class ServicePlatformPermissions {

    private ServicePlatformPermissions() {}

    public static final String VIEW_SERVICE_CATALOG     = "View Service Catalog";
    public static final String MANAGE_SERVICE_CATALOG   = "Manage Service Catalog";
    public static final String VIEW_SERVICE_PROVIDERS   = "View Service Providers";
    public static final String MANAGE_SERVICE_PROVIDERS = "Manage Service Providers";
    public static final String CREATE_SERVICE_REQUEST   = "Create Service Request";
    public static final String VIEW_SERVICE_REQUESTS    = "View Service Requests";
    public static final String MANAGE_SERVICE_REQUESTS  = "Manage Service Requests";
    public static final String VIEW_WORK_ORDERS         = "View Work Orders";
    public static final String MANAGE_WORK_ORDERS       = "Manage Work Orders";

    public static final List<String> ALL = List.of(
            VIEW_SERVICE_CATALOG, MANAGE_SERVICE_CATALOG,
            VIEW_SERVICE_PROVIDERS, MANAGE_SERVICE_PROVIDERS,
            CREATE_SERVICE_REQUEST, VIEW_SERVICE_REQUESTS, MANAGE_SERVICE_REQUESTS,
            VIEW_WORK_ORDERS, MANAGE_WORK_ORDERS
    );
}
