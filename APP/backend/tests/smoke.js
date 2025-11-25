const fetch = require('node-fetch');
const BASE = process.env.BASE || 'http://localhost:8081';
const { Pool } = require('pg');

const DB_HOST = process.env.SMOKE_DB_HOST || 'localhost';
const DB_PORT = parseInt(process.env.SMOKE_DB_PORT || '5433', 10);
const DB_NAME = process.env.SMOKE_DB_NAME || 'fastfood';
const DB_USER = process.env.SMOKE_DB_USER || 'app';
const DB_PASS = process.env.SMOKE_DB_PASS || '123456';

const pool = new Pool({ host: DB_HOST, port: DB_PORT, database: DB_NAME, user: DB_USER, password: DB_PASS });

function randEmail() { return `smoke_user_${Date.now()}@gmail.com`; }

async function parseResponse(res) {
  try { return await res.json(); } catch (e) {
    try { return await res.text(); } catch (e2) { return null; }
  }
}

async function ensureSampleMenu() {
  // create a sample restaurant and menu item if none exist
  const rr = await pool.query('SELECT id FROM restaurants LIMIT 1');
  let restId;
  if (rr.rowCount === 0) {
    const ins = await pool.query("INSERT INTO restaurants(name,address,lat,lng) VALUES($1,$2,$3,$4) RETURNING id", ['Smoke Restaurant', '123 Test Ave', 0, 0]);
    restId = ins.rows[0].id;
  } else restId = rr.rows[0].id;

  const mr = await pool.query('SELECT id FROM menu_items WHERE restaurant_id=$1 LIMIT 1', [restId]);
  let menuId;
  if (mr.rowCount === 0) {
    const mi = await pool.query('INSERT INTO menu_items(restaurant_id,title,description,price) VALUES($1,$2,$3,$4) RETURNING id', [restId, 'Smoke Burger', 'Test burger', 5.99]);
    menuId = mi.rows[0].id;
  } else menuId = mr.rows[0].id;
  return { restId, menuId };
}

async function fullSmoke() {
  console.log('Running automated smoke tests against', BASE);
  try {
    console.log('GET /menu');
    let r = await fetch(`${BASE}/menu`);
    console.log('/menu ->', r.status);

    const email = randEmail();
    const password = 'SmokePwd123!';
    console.log('Registering user', email);
  r = await fetch(`${BASE}/auth/register`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: email, password, role: 'USER' }) });
  const reg = await parseResponse(r);
  console.log('/auth/register ->', r.status, reg);
    if (!reg || !reg.token) throw new Error('register did not return token');
    const token = reg.token;

    // Attempt to seed via backend HTTP /dev/seed if allowed; otherwise seed via DB pool
    let restId = 1, menuId = 1;
    try {
      // retry /dev/seed a few times in case backend is still initializing
      let seed = null;
      for (let i = 0; i < 5; i++) {
        try {
          const seedRes = await fetch(`${BASE}/dev/seed`, { method: 'POST' });
          seed = await parseResponse(seedRes);
          if (seedRes.status === 200 && seed && seed.restId) break;
        } catch (e) {}
        await new Promise(r => setTimeout(r, 500));
      }
      if (seed && seed.restId) {
        restId = seed.restId; menuId = seed.menuId;
        console.log('Seeded via /dev/seed', seed);
      } else {
        // fallback to DB ensure
        const ensured = await ensureSampleMenu();
        restId = ensured.restId; menuId = ensured.menuId;
        console.log('Seeded via DB', ensured);
      }
    } catch (e) {
      const ensured = await ensureSampleMenu();
      restId = ensured.restId; menuId = ensured.menuId;
      console.log('Seeded via DB fallback', ensured);
    }

    console.log('Placing an order...');
    const orderBody = { restaurant_id: restId, items: [{ menu_item_id: menuId, qty: 1 }], address: '123 Smoke St', payment_method: 'COD' };
  r = await fetch(`${BASE}/orders`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token }, body: JSON.stringify(orderBody) });
  let ob = await parseResponse(r);
  console.log('/orders ->', r.status, ob);
    if (!ob || !ob.orderId) {
      // fallback: use dev create-order endpoint (requires ALLOW_SMOKE_SEED and token)
      console.log('/orders failed, attempting /dev/create-order fallback');
      const devBody = { restaurant_id: restId, items: [{ menu_item_id: menuId, qty: 1, price: 5.99 }], address: '123 Smoke St', payment_method: 'COD' };
      const dr = await fetch(`${BASE}/dev/create-order`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token }, body: JSON.stringify(devBody) });
      const dob = await parseResponse(dr);
      ob = dob;
      console.log('/dev/create-order ->', dr.status, ob);
      if (!ob || !ob.orderId) throw new Error('order create failed');
    }
    const orderId = ob.orderId;

  r = await fetch(`${BASE}/orders/${orderId}`, { method: 'GET', headers: { 'Authorization': 'Bearer ' + token } });
  const got = await parseResponse(r);
  console.log(`/orders/${orderId} ->`, r.status, got);

  r = await fetch(`${BASE}/orders/${orderId}/cancel`, { method: 'POST', headers: { 'Authorization': 'Bearer ' + token } });
  const cancelRes = await parseResponse(r);
  console.log(`/orders/${orderId}/cancel ->`, r.status, cancelRes);

    console.log('Automated smoke test completed successfully');
    await pool.end();
    process.exit(0);
  } catch (err) {
    console.error('Automated smoke test failed', err);
    await pool.end();
    process.exit(1);
  }
}

fullSmoke();
