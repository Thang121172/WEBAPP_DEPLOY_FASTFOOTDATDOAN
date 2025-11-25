const fetch = require('node-fetch');
const { Pool } = require('pg');

const BASE = process.env.BASE || 'http://localhost:8082';

const DB_HOST = process.env.SMOKE_DB_HOST || 'localhost';
const DB_PORT = parseInt(process.env.SMOKE_DB_PORT || '5433', 10);
const DB_NAME = process.env.SMOKE_DB_NAME || 'fastfood';
const DB_USER = process.env.SMOKE_DB_USER || 'app';
const DB_PASS = process.env.SMOKE_DB_PASS || '123456';

const pool = new Pool({ host: DB_HOST, port: DB_PORT, database: DB_NAME, user: DB_USER, password: DB_PASS });

function randEmail() { return `refund_test_${Date.now()}@gmail.com`; }

async function parseResponse(res) {
  try { return await res.json(); } catch (e) {
    try { return await res.text(); } catch (e2) { return null; }
  }
}

async function ensureSampleMenu() {
  const rr = await pool.query('SELECT id FROM restaurants LIMIT 1');
  let restId;
  if (rr.rowCount === 0) {
    const ins = await pool.query("INSERT INTO restaurants(name,address,lat,lng) VALUES($1,$2,$3,$4) RETURNING id", ['Refund Restaurant', '1 Refund Ave', 0, 0]);
    restId = ins.rows[0].id;
  } else restId = rr.rows[0].id;

  const mr = await pool.query('SELECT id FROM menu_items WHERE restaurant_id=$1 LIMIT 1', [restId]);
  let menuId;
  if (mr.rowCount === 0) {
    const mi = await pool.query('INSERT INTO menu_items(restaurant_id,title,description,price) VALUES($1,$2,$3,$4) RETURNING id', [restId, 'Refund Burger', 'Refund test burger', 6.99]);
    menuId = mi.rows[0].id;
  } else menuId = mr.rows[0].id;
  return { restId, menuId };
}

async function run() {
  console.log('Running refund test against', BASE);
  try {
    const email = randEmail();
    const password = 'RefundPwd123!';
    console.log('Registering user', email);
    let r = await fetch(`${BASE}/auth/register`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: email, password, role: 'USER' }) });
    const reg = await parseResponse(r);
    console.log('/auth/register ->', r.status, reg);
    if (!reg || !reg.token) throw new Error('register did not return token');
    const token = reg.token;

    // ensure menu
    const ensured = await ensureSampleMenu();
    const restId = ensured.restId; const menuId = ensured.menuId;
    console.log('Using restaurant/menu', restId, menuId);

    // create ONLINE paid order via dev/create-order (requires ALLOW_SMOKE_SEED=true on server)
    console.log('Creating ONLINE order via /dev/create-order');
    const devBody = { restaurant_id: restId, items: [{ menu_item_id: menuId, qty: 1, price: 6.99 }], address: '1 Refund Ave', payment_method: 'ONLINE' };
    r = await fetch(`${BASE}/dev/create-order`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token }, body: JSON.stringify(devBody) });
    const dob = await parseResponse(r);
    console.log('/dev/create-order ->', r.status, dob);
    if (!dob || !dob.orderId) throw new Error('dev create-order failed');
    const orderId = dob.orderId;

    // cancel the order
    console.log('Cancelling order', orderId);
    r = await fetch(`${BASE}/orders/${orderId}/cancel`, { method: 'POST', headers: { 'Authorization': 'Bearer ' + token } });
    const cancelRes = await parseResponse(r);
    console.log(`/orders/${orderId}/cancel ->`, r.status, cancelRes);

    // query payments table for refund
    const pr = await pool.query('SELECT * FROM payments WHERE order_id=$1 ORDER BY id DESC LIMIT 5', [orderId]);
    console.log('Payments rows for order:', pr.rowCount);
    if (pr.rowCount === 0) {
      console.error('No payment records found for order — expected refund record');
      process.exit(2);
    }
    const refunds = pr.rows.filter(p => (p.method && p.method.toUpperCase().includes('REF')) || (p.status && p.status.toUpperCase().includes('REF')));
    if (refunds.length === 0) {
      console.error('No refund-style payment record found. Rows:', pr.rows);
      process.exit(3);
    }
    console.log('Found refund records:', refunds);
    await pool.end();
    console.log('Refund test succeeded');
    process.exit(0);
  } catch (err) {
    console.error('Refund test failed', err);
    await pool.end();
    process.exit(1);
  }
}

run();
