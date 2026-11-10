package com.manacommunity.api.fresh.inventory.model;

public enum StockMovementType {
    INWARD_RECEIPT,
    ORDER_RESERVE,
    ORDER_PICK,
    ORDER_CANCEL_RESTOCK,
    WASTAGE_WRITE_OFF,
    RECALL_QUARANTINE,
    AUDIT_ADJUSTMENT
}
