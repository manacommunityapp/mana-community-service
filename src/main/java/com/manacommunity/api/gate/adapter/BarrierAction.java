package com.manacommunity.api.gate.adapter;

public enum BarrierAction {
    OPEN,  // Allow passage
    HOLD,  // Stop and notify guard
    DENY   // Explicitly blocked
}
