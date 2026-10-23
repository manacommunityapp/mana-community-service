package com.manacommunity.api.iot.metering;

public class MeteringEnums {
    public enum MeterType {
        WATER_METER,
        ELECTRICITY_METER,
        GAS_METER,
        DIESEL_GENERATOR
    }

    public enum MeterProtocol {
        MQTT,
        MODBUS_TCP,
        MODBUS_RTU,
        HTTP_PULSE
    }

    public enum MeterStatus {
        ACTIVE,
        TAMPERED,
        OFFLINE,
        FAULT
    }

    public enum MeterBillingStatus {
        PENDING_SYNC,
        BILLED_IN_CFBOS,
        DISPUTED
    }
}
