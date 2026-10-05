package com.manacommunity.api.sync;

public class SyncEnums {
    public enum SyncEntityType {
        VISITOR_PASS,
        PARKING_ENTRY,
        SOS_TRIGGER,
        HELPDESK_TICKET,
        EV_SESSION,
        GENERAL
    }

    public enum SyncOperation {
        CREATE,
        UPDATE,
        DELETE
    }

    public enum SyncStatus {
        APPLIED,
        CONFLICT_SERVER_WINS,
        REJECTED
    }

    public enum ConflictResolutionStrategy {
        SERVER_WINS,
        CLIENT_WINS_LWW // Last-Write-Wins based on client timestamp
    }
}
