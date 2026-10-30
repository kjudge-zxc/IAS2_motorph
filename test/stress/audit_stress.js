// Control 1 (audit logging) adversarial tests with k6
// Run the LoginTestServer first, then:
// k6 run audit_stress.js                    -> Scenario 1: burst of failed logins (ramping VUs)
// k6 run -e MODE=oversized audit_stress.js  -> Scenario 2: oversized-email attempts

import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = 'http://127.0.0.1:8080';
const MODE = __ENV.MODE || 'burst';
const attempts = new Counter('login_attempts_sent');

// 401 = login refused, which is the expected answer here (not an HTTP error)
http.setResponseCallback(http.expectedStatuses(200, 401));

export const options = MODE === 'oversized'
  ? { scenarios: { oversized: { executor: 'shared-iterations', vus: 5, iterations: 50 } } }
  : { scenarios: { burst: {
        executor: 'ramping-vus',
        startVUs: 1,
        stages: [
          { duration: '15s', target: 10 },   // normal busy morning
          { duration: '15s', target: 50 },   // heavy load
          { duration: '15s', target: 100 },  // attack-level burst
          { duration: '15s', target: 100 },  // hold at peak
          { duration: '5s',  target: 0 },    // ramp down
        ],
        gracefulRampDown: '5s',
      } } };

export function setup() {
  return { prefix: `${MODE}.run${Date.now() % 100000}.` };
}

export default function (data) {
  // Unique fake email per attempt so every request should produce exactly one audit row
  const id = `vu${__VU}.it${__ITER}`;
  const email = MODE === 'oversized'
    ? `${data.prefix}${id}.` + 'a'.repeat(120) + '@motorph.test'   // > 100 chars, valid format
    : `${data.prefix}${id}@motorph.test`;

  const res = http.post(`${BASE}/login`, { email: email, password: 'WrongPass!' });
  attempts.add(1);
  check(res, { 'login refused (401)': (r) => r.status === 401 });
}

export function teardown(data) {
  const res = http.get(`${BASE}/audit-count?prefix=${encodeURIComponent(data.prefix)}`);
  console.log(`AUDIT CHECK -> prefix "${data.prefix}" | rows recorded in audit_log: ${res.json('recorded')} ` +
              `(compare with login_attempts_sent in the summary below)`);
}