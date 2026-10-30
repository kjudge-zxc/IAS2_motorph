// Control 2 (account lockout) adversarial test with k6
// Simulates a parallel brute-force attack: many attackers guess passwords for
// ONE account at the same moment. The lockout should allow at most 5 guesses.
//
// Run the LoginTestServer first, then (use your test account's email):
// k6 run -e EMAIL=your.test.account@motorph.com lockout_stress.js

import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = 'http://127.0.0.1:8080';
const EMAIL = __ENV.EMAIL;
const guesses = new Counter('password_guesses_sent');

// 401 = login refused (expected). 200 would mean a wrong password got in.
http.setResponseCallback(http.expectedStatuses(200, 401));

export const options = {
  scenarios: {
    parallel_bruteforce: {
      executor: 'per-vu-iterations',
      vus: 50,          // 50 attackers start at the same time
      iterations: 4,    // each tries 4 wrong passwords -> 200 guesses in total
      maxDuration: '1m',
    },
  },
};

export function setup() {
  if (!EMAIL) throw new Error('Add the target account: k6 run -e EMAIL=... lockout_stress.js');
}

export default function () {
  const res = http.post(`${BASE}/login`, {
    email: EMAIL,
    password: `WrongGuess_${__VU}_${__ITER}`,   // always wrong, never the real password
  });
  guesses.add(1);
  check(res, { 'login refused (401)': (r) => r.status === 401 });
}