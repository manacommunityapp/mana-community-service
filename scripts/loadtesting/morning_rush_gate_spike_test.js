import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom Metrics for Gate Rush Sizing
const gateLookupDuration = new Trend('gate_lookup_duration_ms', true);
const gateEntryCreationDuration = new Trend('gate_entry_creation_duration_ms', true);
const successfulEntriesRate = new Rate('successful_entries_rate');
const barrierOpenRate = new Rate('barrier_open_rate');
const httpErrorCounter = new Counter('http_error_count');

export const options = {
  scenarios: {
    // Simulating 7:30 AM - 9:30 AM Morning Rush Burst
    morning_gate_rush: {
      executor: 'ramping-vus',
      startVUs: 100,
      stages: [
        { duration: '3m', target: 2000 },  // 7:30 AM - Early domestic help & milk delivery
        { duration: '5m', target: 8200 },  // 8:00 AM - 8:30 AM Peak bell-curve burst (maids + school buses)
        { duration: '10m', target: 8200 }, // Sustained morning rush
        { duration: '5m', target: 2500 },  // 9:00 AM - Tapering commute traffic
        { duration: '2m', target: 200 },   // Post-rush normalization
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    // SLA Thresholds for Gate Tablet Operations (physical barrier cannot lag)
    'http_req_duration': ['p(95)<200', 'p(99)<450'], // 95% of calls under 200ms
    'gate_lookup_duration_ms': ['p(95)<120', 'p(99)<250'],
    'successful_entries_rate': ['rate>0.999'], // >99.9% success
    'http_req_failed': ['rate<0.001'],         // <0.1% failures
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'https://staging-api.mygate.com/api/v1';

export default function () {
  const societyId = 'SOC-MUM-991';
  const flatNumber = `A-${Math.floor(Math.random() * 500) + 101}`;
  const guardToken = 'Bearer mock-guard-jwt-token';

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': guardToken,
    'X-Society-Id': societyId,
  };

  // Step 1: Guard Tablet Plate / Barcode / Visitor Phone Lookup
  const lookupPayload = JSON.stringify({
    societyId: societyId,
    query: `98765${Math.floor(10000 + Math.random() * 90000)}`,
    type: 'PHONE_NUMBER',
  });

  const lookupStart = Date.now();
  const lookupRes = http.post(`${BASE_URL}/gate/lookup`, lookupPayload, { headers });
  gateLookupDuration.add(Date.now() - lookupStart);

  const lookupOk = check(lookupRes, {
    'lookup status is 200': (r) => r.status === 200,
  });

  if (!lookupOk) {
    httpErrorCounter.add(1);
    sleep(0.5);
    return;
  }

  // Step 2: Create Visitor Entry at Physical Gate (triggers Push/VoIP fanout + S3 photo upload)
  const entryPayload = JSON.stringify({
    societyId: societyId,
    flatNumber: flatNumber,
    visitorName: 'Swiggy Delivery Partner',
    visitorPhone: `+9198765${Math.floor(10000 + Math.random() * 90000)}`,
    purpose: 'DELIVERY',
    photoKey: `s3://society-photos/${societyId}/2026-11-28/${Date.now()}.webp`,
    vehicleNumber: 'MH02AB1234',
  });

  const entryStart = Date.now();
  const entryRes = http.post(`${BASE_URL}/gate/entries`, entryPayload, { headers });
  gateEntryCreationDuration.add(Date.now() - entryStart);

  const entryOk = check(entryRes, {
    'entry created status is 201': (r) => r.status === 201,
    'entry has valid entryId': (r) => r.json('entryId') !== undefined,
  });

  successfulEntriesRate.add(entryOk);

  if (entryOk) {
    const entryId = entryRes.json('entryId');

    // Step 3: Guard Tablet Polling / Status check (waiting for Resident Approval or Auto-Pass)
    const statusRes = http.get(`${BASE_URL}/gate/entries/${entryId}/status`, { headers });
    const statusOk = check(statusRes, {
      'status poll is 200': (r) => r.status === 200,
    });

    if (statusOk && statusRes.json('status') === 'APPROVED') {
      barrierOpenRate.add(1);
    }
  } else {
    httpErrorCounter.add(1);
  }

  // Pacing: guards process entries every 1-3 seconds per lane
  sleep(Math.random() * 2 + 1);
}
