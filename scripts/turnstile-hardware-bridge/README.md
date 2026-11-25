# IoT Turnstile Hardware Relay Bridge

Standalone edge microservice for interfacing physical pedestrian turnstiles, flap barriers, speed gates, and magnetic door locks with Mana Community's AI biometric access control and attendance kiosk platform.

## Features
- **Plug-and-Play Relay Drivers**:
  - `mock`: Local testing, CI/CD pipeline, and non-hardware staging.
  - `gpio`: Direct dry-contact 12V/24V switching via Raspberry Pi BCM GPIO pins (`RPi.GPIO`).
  - `http`: Web relay boards (Shelly, Sonoff Tasmota, ControlByWeb WebRelay).
- **Auto-pulse Unlock**: Sends a 3000ms timed closure to allow pedestrian entry and automatically re-locks.
- **Fail-Safe Design**: If network connectivity drops, local kiosk offline queues preserve clock-ins while physical gates remain operable via emergency overrides.

## Quickstart

### 1. Test Mode (Mock)
```bash
python turnstile_edge_bridge.py --driver mock --turnstiles TST-MAIN-01,TST-MAIN-02
```

### 2. Raspberry Pi Hardware Installation
```bash
# Connect relay module IN1 -> GPIO 17, IN2 -> GPIO 27, VCC -> 5V, GND -> GND
# Connect relay NO & COM to the Turnstile Motherboard "OPEN_A" & "GND" terminals
python3 turnstile_edge_bridge.py \
  --host http://192.168.1.10:8080 \
  --community-id 1 \
  --driver gpio \
  --gpio-pins "TST-MAIN-01:17,TST-MAIN-02:27"
```

### 3. IP Network Relay (Shelly / WebRelay)
```bash
python3 turnstile_edge_bridge.py \
  --host http://192.168.1.10:8080 \
  --driver http \
  --http-url "http://192.168.1.50/relay/0?turn=on&timer={duration_ms}"
```
