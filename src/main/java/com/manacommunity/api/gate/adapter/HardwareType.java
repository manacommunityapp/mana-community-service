package com.manacommunity.api.gate.adapter;

public enum HardwareType {
    ANPR,        // Automatic Number Plate Recognition camera
    RFID,        // RFID sticker / fob reader
    SMART_LOCK,  // Wi-Fi / BLE smart door lock
    BOOM_BARRIER,// Relay-controlled boom barrier
    QR_SCAN,     // Guard-tablet QR scan (software event)
    MANUAL       // Guard manual override
}
