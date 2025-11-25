const { spawn } = require('child_process');
const fetch = require('node-fetch');
const path = require('path');

const BASE_PORT = process.env.PORT || 8080;
const BASE = `http://localhost:${BASE_PORT}`;

function waitFor(url, timeoutMs = 30000) {
  const start = Date.now();
  return new Promise((resolve, reject) => {
    const tryFetch = async () => {
      try {
        const r = await fetch(url);
        if (r.status === 200) return resolve(true);
      } catch (e) {}
      if (Date.now() - start > timeoutMs) return reject(new Error('timeout'));
      setTimeout(tryFetch, 1000);
    };
    tryFetch();
  });
}

async function run() {
  console.log('Starting backend...');
  const childEnv = { ...process.env, PORT: BASE_PORT, ALLOW_SMOKE_SEED: 'true' };
  // set DB env defaults for spawned backend if not present
  childEnv.DB_HOST = childEnv.DB_HOST || 'localhost';
  childEnv.DB_PORT = childEnv.DB_PORT || '5433';
  childEnv.DB_NAME = childEnv.DB_NAME || 'fastfood';
  childEnv.DB_USER = childEnv.DB_USER || 'app';
  childEnv.DB_PASSWORD = childEnv.DB_PASSWORD || '123456';
  // ensure tests also use the same DB connection settings (tests use SMOKE_DB_* env vars)
  childEnv.SMOKE_DB_HOST = childEnv.SMOKE_DB_HOST || childEnv.DB_HOST;
  childEnv.SMOKE_DB_PORT = childEnv.SMOKE_DB_PORT || childEnv.DB_PORT;
  childEnv.SMOKE_DB_NAME = childEnv.SMOKE_DB_NAME || childEnv.DB_NAME;
  childEnv.SMOKE_DB_USER = childEnv.SMOKE_DB_USER || childEnv.DB_USER;
  childEnv.SMOKE_DB_PASS = childEnv.SMOKE_DB_PASS || childEnv.DB_PASSWORD;
  // ensure tests target the backend URL
  childEnv.BASE = childEnv.BASE || `http://localhost:${BASE_PORT}`;
  const proc = spawn('node', ['index.js'], { cwd: path.join(__dirname,'..'), stdio: 'inherit', env: childEnv });
  try {
    await waitFor(`${BASE}/menu`, 30000);
    console.log('Backend ready, running tests');
  const smoke = spawn('node', ['tests/smoke.js'], { cwd: path.join(__dirname,'..'), stdio: 'inherit', env: childEnv });
    await new Promise((res) => smoke.on('exit', (c) => res(c)));
  const refund = spawn('node', ['tests/refund_test.js'], { cwd: path.join(__dirname,'..'), stdio: 'inherit', env: childEnv });
    await new Promise((res) => refund.on('exit', (c) => res(c)));
  const logout = spawn('node', ['tests/logout_test.js'], { cwd: path.join(__dirname,'..'), stdio: 'inherit', env: childEnv });
    const logoutCode = await new Promise((res) => logout.on('exit', (c) => res(c)));
    if (logoutCode === 0) process.exit(0); else process.exit(2);
  } catch (e) {
    console.error('Test run failed', e);
    process.exit(2);
  } finally {
    try { proc.kill(); } catch (e) {}
  }
}

run();
