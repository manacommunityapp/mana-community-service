package com.manacommunity.api.constants;

import com.manacommunity.api.constants.permissions.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleConstants {

    private ModuleConstants() {}

    public record ModuleDef(String key, String label, int sortOrder) {}

    public static final String MODULE_COMMUNITY_FEED    = "COMMUNITY_FEED";
    public static final String MODULE_SPORTS            = "SPORTS";
    public static final String MODULE_MARKETPLACE       = "MARKETPLACE";
    public static final String MODULE_VISITORS          = "VISITORS";
    public static final String MODULE_NOTICES           = "NOTICES";
    public static final String MODULE_BOOKINGS          = "BOOKINGS";
    public static final String MODULE_HELPDESK          = "HELPDESK";
    public static final String MODULE_POLLS             = "POLLS";
    public static final String MODULE_JOBS              = "JOBS";
    public static final String MODULE_EVENTS            = "EVENTS";
    public static final String MODULE_COMMUNITY_MGMT   = "COMMUNITY_MGMT";
    public static final String MODULE_FINANCE_MGMT     = "FINANCE_MGMT";
    public static final String MODULE_ADMIN_HUB        = "ADMIN_HUB";
    public static final String MODULE_FOOD_OS          = "FOOD_OS";
    public static final String MODULE_VENDOR_MANAGEMENT = "VENDOR_MANAGEMENT";

    public static final List<ModuleDef> ALL_MODULES = List.of(
        new ModuleDef(MODULE_COMMUNITY_FEED,    "Community Feed",    1),
        new ModuleDef(MODULE_SPORTS,            "Sports",            2),
        new ModuleDef(MODULE_MARKETPLACE,       "Marketplace",       3),
        new ModuleDef(MODULE_VISITORS,          "Visitors",          4),
        new ModuleDef(MODULE_NOTICES,           "Notices",           5),
        new ModuleDef(MODULE_BOOKINGS,          "Bookings",          6),
        new ModuleDef(MODULE_HELPDESK,          "Helpdesk",          7),
        new ModuleDef(MODULE_POLLS,             "Polls",             8),
        new ModuleDef(MODULE_JOBS,              "Jobs & Referrals",  9),
        new ModuleDef(MODULE_EVENTS,            "Events",           10),
        new ModuleDef(MODULE_COMMUNITY_MGMT,    "Community Mgmt",   11),
        new ModuleDef(MODULE_FINANCE_MGMT,      "Finance Mgmt",     12),
        new ModuleDef(MODULE_ADMIN_HUB,         "Admin Hub",        13),
        new ModuleDef(MODULE_FOOD_OS,           "Food & Lifestyle", 14),
        new ModuleDef(MODULE_VENDOR_MANAGEMENT, "Vendor Management",15)
    );

    public static final Map<String, String> PERMISSION_TO_MODULE;

    static {
        Map<String, String> map = new HashMap<>();
        CommunityFeedPermissions.ALL.forEach(p -> map.put(p, MODULE_COMMUNITY_FEED));
        SportsPermissions.ALL.forEach(p         -> map.put(p, MODULE_SPORTS));
        MarketplacePermissions.ALL.forEach(p    -> map.put(p, MODULE_MARKETPLACE));
        VisitorPermissions.ALL.forEach(p        -> map.put(p, MODULE_VISITORS));
        NoticeBoardPermissions.ALL.forEach(p    -> map.put(p, MODULE_NOTICES));
        AmenityPermissions.ALL.forEach(p        -> map.put(p, MODULE_BOOKINGS));
        HelpdeskPermissions.ALL.forEach(p       -> map.put(p, MODULE_HELPDESK));
        PollingPermissions.ALL.forEach(p        -> map.put(p, MODULE_POLLS));
        JobsPermissions.ALL.forEach(p           -> map.put(p, MODULE_JOBS));
        EventPermissions.ALL.forEach(p          -> map.put(p, MODULE_EVENTS));
        AdminPermissions.ALL.forEach(p          -> map.put(p, MODULE_ADMIN_HUB));
        ServicePlatformPermissions.ALL.forEach(p -> map.put(p, MODULE_FINANCE_MGMT));
        FoodPermissions.ALL.forEach(p           -> map.put(p, MODULE_FOOD_OS));
        VendorPermissions.ALL.forEach(p         -> map.put(p, MODULE_VENDOR_MANAGEMENT));
        PERMISSION_TO_MODULE = Collections.unmodifiableMap(map);
    }

    public static String getModuleForPermission(String permissionKey) {
        return PERMISSION_TO_MODULE.get(permissionKey);
    }
}
