package com.manacommunity.api.devices.adapter;

import com.manacommunity.api.devices.entity.SmartDevice;

public interface SmartDeviceApiAdapter {
    AdapterResult execute(SmartDevice device, String action);

    record AdapterResult(String result, String errorMessage, int durationMs) {}
}
