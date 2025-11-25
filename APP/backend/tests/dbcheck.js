const { Pool } = require('pg');
(async ()=>{
  const pool = new Pool({host:'localhost',port:5433,database:'fastfood',user:'app',password:'123456'});
  try {
    const r = await pool.query("SELECT id,username,email,created_at FROM users WHERE username LIKE 'smoke_user_%' ORDER BY id DESC LIMIT 10");
    console.log('rows:', r.rows);
  } catch (e) { console.error(e); }
  await pool.end();
})();
