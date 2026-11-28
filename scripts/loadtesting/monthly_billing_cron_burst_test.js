import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom Metrics for Bulk Billing Burst
const pdfRenderDuration = new Trend('pdf_render_duration_ms', true);
const dbWriteLatency = new Trend('db_write_latency_ms', true);
const invoicesDispatched = new Counter('invoices_dispatched_total');
const billingErrorRate = new Rate('billing_error_rate');

export const options = {
  scenarios: {
    // 1st of the month: 10M invoices within 4 hours (target 926 invoices/sec)
    monthly_billing_blast: {
      executor: 'constant-arrival-rate',
      rate: 950, // 950 iterations per second
      timeUnit: '1s',
      duration: '15m', // representative 15-minute slice of the 3-hour batch
      preAllocatedVUs: 150,
      maxVUs: 300,
    },
  },
  thresholds: {
    'http_req_duration': ['p(95)<300', 'p(99)<600'],
    'pdf_render_duration_ms': ['p(95)<50', 'p(99)<100'],
    'db_write_latency_ms': ['p(95)<25', 'p(99)<50'],
    'billing_error_rate': ['rate<0.0001'], // 99.99% accuracy
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'https://staging-api.mygate.com/api/v1';

export default function () {
  const societyId = `SOC-${Math.floor(Math.random() * 500) + 1}`;
  const flatNumber = `B-${Math.floor(Math.random() * 400) + 101}`;
  const workerAuth = 'Bearer billing-batch-worker-jwt-secret';

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': workerAuth,
  };

  const payload = JSON.stringify({
    societyId: societyId,
    flatNumber: flatNumber,
    billingPeriod: '2026-11',
    carpetAreaSqFt: 1450.0,
    parkingSlots: 1,
    waterUnitsConsumed: 12.5,
    residentAdvanceWalletBalance: Math.random() > 0.8 ? 5000.0 : 0.0, // 20% flats have advance balance
  });

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}/society-erp/invoices/generate`, payload, { headers });
  const latency = Date.now() - startTime;

  dbWriteLatency.add(latency * 0.3); // simulated DB commit portion
  pdfRenderDuration.add(latency * 0.5); // simulated PDF rendering & signing portion

  const success = check(res, {
    'invoice generated status is 201': (r) => r.status === 201,
    'invoice has valid ID': (r) => r.json('invoiceId') !== undefined,
    'gross amount is positive': (r) => r.json('grossTotalAmount') > 0,
  });

  if (success) {
    invoicesDispatched.add(1);
    billingErrorRate.add(0);
  } else {
    billingErrorRate.add(1);
  }
}
