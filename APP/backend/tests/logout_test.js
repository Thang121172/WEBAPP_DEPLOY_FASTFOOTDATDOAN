const fetch = require('node-fetch');
const BASE = process.env.BASE || 'http://localhost:8082';

async function parseResponse(res) {
  try { return await res.json(); } catch (e) { try { return await res.text(); } catch (e2) { return null; } }
}

async function logoutTest() {
  console.log('Running logout test against', BASE);
  const email = `logout_test_${Date.now()}@gmail.com`;
  const password = 'LogoutPwd123!';
  // register
  let r = await fetch(`${BASE}/auth/register`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: email, password, role: 'USER' }) });
  const reg = await parseResponse(r);
  console.log('/auth/register ->', r.status, reg);
  if (!reg || !reg.token) { console.error('register failed'); process.exit(2); }
  const token = reg.token;

  // call protected endpoint (create an order will exercise auth) - fallback to GET /menu (public) then admin ping isn't appropriate
  // We'll call /orders (which requires auth) with invalid body and expect 400 (auth succeeded)
  r = await fetch(`${BASE}/orders`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token }, body: JSON.stringify({ restaurant_id: 1, items: [{ menu_item_id: 1, qty: 1 }], address: 'x', payment_method: 'COD' }) });
  console.log('/orders initial ->', r.status);
  if (r.status === 401) { console.error('Auth failed unexpectedly'); process.exit(2); }

  // logout
  r = await fetch(`${BASE}/auth/logout`, { method: 'POST', headers: { 'Authorization': 'Bearer ' + token } });
  const out = await parseResponse(r);
  console.log('/auth/logout ->', r.status, out);
  if (r.status !== 200) { console.error('logout failed'); process.exit(2); }

  // attempt protected call again with same token
  r = await fetch(`${BASE}/orders`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token }, body: JSON.stringify({ restaurant_id: 1, items: [{ menu_item_id: 1, qty: 1 }], address: 'x', payment_method: 'COD' }) });
  console.log('/orders after logout ->', r.status);
  if (r.status === 401) {
    console.log('Logout test passed');
    process.exit(0);
  } else {
    console.error('Logout test failed: token still valid');
    process.exit(2);
  }
}

logoutTest();
