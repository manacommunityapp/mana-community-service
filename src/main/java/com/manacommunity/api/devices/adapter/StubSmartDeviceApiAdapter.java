package com.manacommunity.api.devices.adapter;

import com.manacommunity.api.devices.entity.SmartDevice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Primary  // override with a real vendor bean (HikvisionAdapter, GodrejAdapter, etc.)
public class StubSmartDeviceApiAdapter implements SmartDeviceApiAdapter {

    @Override
    public AdapterResult execute(SmartDevice device, String action) {
        log.info("STUB device control: deviceId={} type={} vendor={} action={}",
                device.getId(), device.getDeviceType(), device.getVendor(), action);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new AdapterResult("SUCCESS", null, 100);
    }
}
