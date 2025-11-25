require('dotenv').config({ override: true });
const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const { Pool } = require('pg');
const http = require('http');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const app = express();
const server = http.createServer(app);
const { Server } = require('socket.io');
const io = new Server(server, { cors: { origin: '*' } });

// If behind a reverse proxy (e.g., Nginx/Ingress), trust X-Forwarded-* headers for req.ip
app.set('trust proxy', 1);

app.use(cors());

// Capture raw body for debugging JSON parse errors and still parse JSON normally
app.use(
  bodyParser.json({
    verify: (req, res, buf) => {
      try {
        req.rawBody = buf.toString();
      } catch (e) {
        req.rawBody = undefined;
      }
    },
  })
);

// ---------- File Upload Configuration ----------
// Tạo thư mục uploads nếu chưa có
const uploadsDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

// Cấu hình multer để lưu file
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadsDir);
  },
  filename: (req, file, cb) => {
    // Tạo tên file unique: timestamp-random-originalname
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    const ext = path.extname(file.originalname);
    cb(null, `product-${uniqueSuffix}${ext}`);
  }
});

const upload = multer({
  storage: storage,
  limits: {
    fileSize: 5 * 1024 * 1024 // 5MB limit
  },
  fileFilter: (req, file, cb) => {
    // Chỉ cho phép image files
    const allowedTypes = /jpeg|jpg|png|gif|webp/;
    const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
    const mimetype = allowedTypes.test(file.mimetype);
    
    if (mimetype && extname) {
      return cb(null, true);
    } else {
      cb(new Error('Chỉ cho phép upload file ảnh (JPEG, PNG, GIF, WebP)'));
    }
  }
});

// Static file serving - để truy cập ảnh qua URL
app.use('/uploads', express.static(uploadsDir));

/** ====================== *
 * Unified DB config
 * - Prefer DB_* from .env
 * - Fallback to PG* (node-postgres defaults)
 * - Fallback to POSTGRES_* (docker-compose style)
 * ====================== */
const DB_HOST =
  process.env.DB_HOST ||
  process.env.PGHOST ||
  process.env.POSTGRES_HOST ||
  '127.0.0.1';
const DB_PORT = parseInt(
  process.env.DB_PORT ||
    process.env.PGPORT ||
    process.env.POSTGRES_PORT ||
    '5432',
  10
);
const DB_NAME =
  process.env.DB_NAME ||
  process.env.POSTGRES_DB || // your .env uses this
  process.env.PGDATABASE ||
  'fastfood';
const DB_USER =
  process.env.DB_USER ||
  process.env.PGUSER ||
  process.env.POSTGRES_USER ||
  'app';
const DB_PASSWORD =
  process.env.DB_PASSWORD ||
  process.env.PGPASSWORD ||
  process.env.POSTGRES_PASSWORD ||
  '123456';

const pool = new Pool({
  host: DB_HOST,
  port: DB_PORT,
  database: DB_NAME,
  user: DB_USER,
  password: DB_PASSWORD,
  // Optional: slightly longer to wait on cold start containers
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 5000,
});

// Log effective DB config (mask password)
console.log('[db] config =', {
  host: DB_HOST,
  port: DB_PORT,
  database: DB_NAME,
  user: DB_USER,
  password: DB_PASSWORD ? '***' : '(empty)',
});

// ---- Startup DB check (non-fatal): helps diagnose 28P01 / ECONNREFUSED, v.v.
(async () => {
  try {
    const client = await pool.connect();
    const r = await client.query(
      'SELECT 1 as ok, current_database() AS db, current_user AS usr'
    );
    console.log('[db] connected OK =>', r.rows[0]);
    client.release();
  } catch (e) {
    console.error('[db] initial connect FAILED');
    console.error('  code   :', e.code);
    console.error('  message:', e.message);
    if (e.detail) console.error('  detail :', e.detail);
    if (e.hint) console.error('  hint   :', e.hint);
  }
})();

const JWT_SECRET = process.env.JWT_SECRET || 'supersecret';
const JWT_ISSUER = process.env.JWT_ISSUER || 'fastfood-app';
const REVOCATION_TTL_DAYS = parseInt(process.env.REVOCATION_TTL_DAYS || '7', 10);
const MAX_OTP_VERIFY_ATTEMPTS = parseInt(
  process.env.MAX_OTP_VERIFY_ATTEMPTS || '5',
  10
);
const REFRESH_TOKEN_TTL_DAYS = parseInt(
  process.env.REFRESH_TOKEN_TTL_DAYS || '30',
  10
);

// ---------- Helpers ----------
function extractTokenPayload(req) {
  const auth = req.headers.authorization;
  if (!auth) return null;
  const parts = auth.split(' ');
  if (parts.length !== 2) return null;
  try {
    return jwt.verify(parts[1], JWT_SECRET);
  } catch (err) {
    return null;
  }
}

function isAllDigits(s) {
  if (!s) return false;
  return /^\d+$/.test(String(s));
}

function isValidEmail(email) {
  if (!email) return false;
  // simple RFC-lite check
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function isGmailAddress(email) {
  if (!isValidEmail(email)) return false;
  const d = String(email).toLowerCase().split('@')[1];
  return d === 'gmail.com' || d === 'googlemail.com';
}

// NEW: normalize email to lowercase & trim
function normEmail(s) {
  return String(s || '').trim().toLowerCase();
}

// Placeholder for issueTokens function (required by verify-otp & login & refresh)
async function issueTokens(user) {
  const payload = {
    id: user.id,
    username: user.username,
    role: user.role,
    jti: require('crypto').randomUUID(), // Unique ID for revocation
  };
  const accessToken = jwt.sign(payload, JWT_SECRET, {
    expiresIn: '1h',
    issuer: JWT_ISSUER,
  });
  const refreshToken = jwt.sign(
    { id: user.id, type: 'refresh' },
    JWT_SECRET,
    { expiresIn: `${REFRESH_TOKEN_TTL_DAYS}d`, issuer: JWT_ISSUER }
  );
  return { accessToken, refreshToken };
}

// ---------- Optional Redis client for distributed rate limiting ----------
// Redis TẮT – chỉ dùng in-memory otpRate Map cho rate limiting
let redis = null;

// ---------- Optional nodemailer for OTP emails ----------
let transporter = null;
try {
  if (process.env.SMTP_HOST && process.env.SMTP_USER) {
    const nodemailer = require('nodemailer');
    const smtpUser = process.env.SMTP_USER
      ? String(process.env.SMTP_USER).trim()
      : undefined;
    const smtpPass = process.env.SMTP_PASS
      ? String(process.env.SMTP_PASS).replace(/\s+/g, '')
      : undefined;
    if (process.env.SMTP_PASS && smtpPass !== process.env.SMTP_PASS) {
      console.log(
        'Note: SMTP_PASS contained whitespace; using sanitized value with spaces removed for authentication.'
      );
    }
    transporter = nodemailer.createTransport({
      host: process.env.SMTP_HOST,
      port: parseInt(process.env.SMTP_PORT || '587', 10),
      secure: process.env.SMTP_SECURE === 'true',
      auth: smtpUser ? { user: smtpUser, pass: smtpPass } : undefined,
      tls: { rejectUnauthorized: false },
    });
  } else {
    console.log(
      'SMTP not configured. OTPs will be logged to console and stored in DB for dev/testing.'
    );
  }
} catch (e) {
  console.error('Nodemailer initialization failed:', e.message);
}

// ---------- Simple in-memory OTP rate limiter ----------
const otpRate = new Map(); // email -> {count, firstTs}
const OTP_LIMIT = parseInt(process.env.OTP_LIMIT || '5', 10);
const OTP_WINDOW_MS = parseInt(
  process.env.OTP_WINDOW_MS || String(60 * 1000),
  10
); // 1 minute
setInterval(() => {
  const now = Date.now();
  for (const [email, rec] of otpRate.entries()) {
    if (now - rec.firstTs > OTP_WINDOW_MS) otpRate.delete(email);
  }
}, 60 * 1000);

// ---------- Middleware ----------
async function requireAuth(req, res, next) {
  const payload = extractTokenPayload(req);
  if (!payload) return res.status(401).json({ error: 'unauthenticated' });
  try {
    if (payload && payload.jti) {
      const rr = await pool.query(
        'SELECT id FROM revoked_tokens WHERE jti=$1 LIMIT 1',
        [payload.jti]
      );
      if (rr.rowCount > 0)
        return res.status(401).json({ error: 'token_revoked' });
    }
    req.user = payload;
    next();
  } catch (e) {
    console.error('requireAuth db error', e);
    res.status(500).json({ error: 'auth_failed' });
  }
}

// ---------- Simple health check ----------
app.get('/health', (req, res) => {
  res.json({ ok: true, version: '1.0.0' });
});

// ---------- Auth: send OTP ----------
app.post('/auth/send-otp', async (req, res) => {
  const email = normEmail(req.body && req.body.email);
  if (!email) return res.status(400).json({ error: 'email required' });
  if (!isValidEmail(email))
    return res.status(400).json({ error: 'invalid_email' });
  if (!isGmailAddress(email))
    return res.status(400).json({ error: 'only_gmail_allowed' });

  // rate limit per email
  try {
    if (redis) {
      const key = `otp:${email}:count`;
      const cur = await redis.incr(key);
      if (cur === 1) {
        await redis.pexpire(key, OTP_WINDOW_MS);
      }
      if (cur > OTP_LIMIT) {
        let ttl = await redis.pttl(key);
        if (ttl < 0) ttl = OTP_WINDOW_MS;
        const retry = Math.max(0, Math.ceil(ttl / 1000));
        res.set('Retry-After', String(retry));
        return res
          .status(429)
          .json({ error: 'otp_rate_limited', retry_after_seconds: retry });
      }
    } else {
      const rec = otpRate.get(email) || { count: 0, firstTs: Date.now() };
      const elapsed = Date.now() - rec.firstTs;
      if (elapsed <= OTP_WINDOW_MS && rec.count >= OTP_LIMIT) {
        const remaining = OTP_WINDOW_MS - elapsed;
        const retry = Math.max(0, Math.ceil(remaining / 1000));
        res.set('Retry-After', String(retry));
        return res
          .status(429)
          .json({ error: 'otp_rate_limited', retry_after_seconds: retry });
      }
      if (elapsed > OTP_WINDOW_MS) {
        rec.count = 0;
        rec.firstTs = Date.now();
      }
      rec.count += 1;
      otpRate.set(email, rec);
    }
  } catch (e) {
    console.error('otp rate limiter error', e);
  }

  try {
    // create or find user by email
    let user = null;
    const r = await pool.query('SELECT id FROM users WHERE email=$1', [email]);
    if (r.rowCount === 0) {
      const create = await pool.query(
        'INSERT INTO users(username,email,role,verified,password) VALUES($1,$2,$3,$4,$5) ON CONFLICT (username) DO UPDATE SET email=EXCLUDED.email RETURNING id',
        [email, email, 'USER', false, '']
      );
      if (create.rowCount > 0) {
        user = { id: create.rows[0].id };
      } else {
        const r2 = await pool.query('SELECT id FROM users WHERE email=$1', [
          email,
        ]);
        if (r2.rowCount === 0) throw new Error('could not create user');
        user = { id: r2.rows[0].id };
      }
    } else {
      user = { id: r.rows[0].id };
    }

    const code = Math.floor(100000 + Math.random() * 900000).toString();
    const codeHash = bcrypt.hashSync(code, 10);
    const expires = new Date(Date.now() + 10 * 60 * 1000); // 10 minutes

    const storePlain =
      process.env.ALLOW_SMOKE_SEED === 'true' ||
      process.env.DEBUG_SHOW_OTP === 'true';
    const plainForDb = storePlain ? code : null;
    await pool.query(
      'INSERT INTO otp_codes(user_id,code,code_hash,expires_at,attempts) VALUES($1,$2,$3,$4,$5)',
      [user.id, plainForDb, codeHash, expires, 0]
    );

    let sendResult = 'console';
    let retryCount = 0;
    const maxRetries = 3;
    const retryDelay = 1000;

    try {
      if (transporter) {
        let sent = false;
        while (!sent && retryCount < maxRetries) {
          try {
            await transporter.sendMail({
              from: process.env.SMTP_FROM || process.env.SMTP_USER,
              to: email,
              subject: 'Your OTP',
              text: `Your OTP is: ${code}`,
            });
            sendResult = 'smtp';
            sent = true;
          } catch (mailErr) {
            retryCount++;
            if (
              retryCount < maxRetries &&
              (mailErr.code === 'ECONNRESET' ||
                mailErr.code === 'ETIMEDOUT' ||
                mailErr.code === 'ENOTFOUND')
            ) {
              console.log(
                `SMTP send failed, retrying in ${
                  retryDelay * retryCount
                }ms (attempt ${retryCount}/${maxRetries})`
              );
              await new Promise((resolve) =>
                setTimeout(resolve, retryDelay * retryCount)
              );
            } else {
              throw mailErr;
            }
          }
        }
        if (!sent) {
          throw new Error('Max retries exceeded');
        }
      } else {
        console.log(
          'SMTP not configured — OTP logged to console. Configure SMTP_HOST, SMTP_USER, etc. for email delivery.'
        );
        console.log(`OTP for ${email}: ${code}`);
      }
    } catch (mailErr) {
      console.error('mailer send error for send-otp', mailErr);
      console.log(`OTP (fallback) for ${email}: ${code}`);
      sendResult = 'fallback';
    }

    // compute remaining quota and window seconds
    try {
      let remaining = OTP_LIMIT;
      let windowSeconds = Math.max(0, Math.ceil(OTP_WINDOW_MS / 1000));
      if (redis) {
        const key = `otp:${email}:count`;
        const curVal = await redis.get(key);
        const cur = curVal ? parseInt(curVal, 10) : 0;
        remaining = Math.max(0, OTP_LIMIT - cur);
        const ttl = await redis.pttl(key);
        if (ttl > 0)
          windowSeconds = Math.max(0, Math.ceil(ttl / 1000));
      } else {
        const rec = otpRate.get(email) || {
          count: 0,
          firstTs: Date.now(),
        };
        remaining = Math.max(0, OTP_LIMIT - rec.count);
        const elapsed = Date.now() - rec.firstTs;
        windowSeconds = Math.max(
          0,
          Math.ceil((OTP_WINDOW_MS - elapsed) / 1000)
        );
      }
      console.log(
        'send-otp stored for userId=',
        user.id,
        'expires=',
        expires,
        'remaining==',
        remaining
      );
      res.set('X-OTP-Remaining', String(remaining));
      res.set('X-OTP-Limit', String(OTP_LIMIT));
      res.set('X-OTP-Window-Seconds', String(windowSeconds));
      console.log(
        JSON.stringify({
          event: 'otp_send',
          email,
          result: sendResult,
          redis_used: !!redis,
          remaining_sends: remaining,
          window_seconds: windowSeconds,
          timestamp: new Date().toISOString(),
        })
      );
    } catch (hdrErr) {
      console.error('send-otp header compute error', hdrErr);
    }

    res.json({ ok: true });
  } catch (err) {
    console.error('send-otp error', err);
    res.status(500).json({ error: 'could not send otp' });
  }
});

// ---------- Dev-only: return last OTP ----------
app.get('/dev/last-otp', async (req, res) => {
  if (process.env.DEBUG_SHOW_OTP !== 'true') {
    return res.status(403).json({ error: 'not_allowed' });
  }
  const clientIP = (
    req.ip ||
    req.connection.remoteAddress ||
    req.socket.remoteAddress ||
    ''
  ).toString();
  const devTokenHeader = req.headers['x-dev-token'];
  const expectedToken = process.env.DEV_TOKEN || null;
  const isPrivate =
    clientIP === '127.0.0.1' ||
    clientIP === '::1' ||
    clientIP.startsWith('192.168.') ||
    clientIP.startsWith('10.') ||
    clientIP.startsWith('172.') ||
    clientIP.startsWith('127.');
  if (!isPrivate) {
    if (!expectedToken) {
      return res.status(403).json({ error: 'dev_token_required' });
    }
    if (!devTokenHeader || devTokenHeader !== expectedToken) {
      return res.status(403).json({ error: 'access_denied' });
    }
  }
  const email = normEmail(req.query && req.query.email);
  if (!email) return res.status(400).json({ error: 'email required' });
  try {
    const ur = await pool.query(
      'SELECT id FROM users WHERE email=$1 LIMIT 1',
      [email]
    );
    if (ur.rowCount === 0)
      return res.status(404).json({ error: 'user not found' });
    const userId = ur.rows[0].id;
    const r = await pool.query(
      'SELECT code,code_hash,expires_at,used,attempts FROM otp_codes WHERE user_id=$1 ORDER BY id DESC LIMIT 1',
      [userId]
    );
    if (r.rowCount === 0)
      return res.status(404).json({ error: 'no_otp_found' });
    const row = r.rows[0];
    if (!row.code) {
      return res.json({
        ok: true,
        note: 'plain code not stored; check server logs or configure SMTP',
        expires_at: row.expires_at,
        used: row.used,
        attempts: row.attempts,
      });
    }
    return res.json({
      ok: true,
      code: row.code,
      expires_at: row.expires_at,
      used: row.used,
      attempts: row.attempts,
    });
  } catch (e) {
    console.error('dev last-otp error', e);
    res.status(500).json({ error: 'failed' });
  }
});

// ---------- Admin OTP metrics ----------
app.get('/admin/otp-metrics', requireAuth, async (req, res) => {
  if (!req.user || req.user.role !== 'ADMIN')
    return res.status(403).json({ error: 'forbidden' });
  try {
    const inMemory = {};
    for (const [email, rec] of otpRate.entries()) {
      const elapsed = Date.now() - rec.firstTs;
      inMemory[email] = {
        count: rec.count,
        firstTs: rec.firstTs,
        remaining: Math.max(0, OTP_LIMIT - rec.count),
        window_seconds_remaining: Math.max(
          0,
          Math.ceil((OTP_WINDOW_MS - elapsed) / 1000)
        ),
      };
    }
    let redisStats = null;
    if (redis) {
      redisStats = {};
      try {
        const keys = await redis.keys('otp:*:count');
        for (const k of keys) {
          try {
            const parts = k.split(':');
            const email = parts[1];
            const val = await redis.get(k);
            const pttl = await redis.pttl(k);
            redisStats[email] = {
              key: k,
              count: val ? parseInt(val, 10) : 0,
              ttl_ms: pttl,
              remaining: Math.max(
                0,
                OTP_LIMIT - (val ? parseInt(val, 10) : 0)
              ),
              window_seconds_remaining:
                pttl > 0 ? Math.max(0, Math.ceil(pttl / 1000)) : 0,
            };
          } catch (e) {
            console.error(
              'otp metrics redis parse error for key',
              k,
              e
            );
          }
        }
      } catch (e) {
        console.error('otp metrics redis keys error', e);
      }
    }
    res.json({ inMemory, redis: redisStats });
  } catch (err) {
    console.error('admin otp-metrics error', err);
    res.status(500).json({ error: 'failed' });
  }
});

app.post('/admin/otp-metrics/reset', requireAuth, async (req, res) => {
  if (!req.user || req.user.role !== 'ADMIN')
    return res.status(403).json({ error: 'forbidden' });
  const email = normEmail(req.body && req.body.email);
  if (!email) return res.status(400).json({ error: 'email required' });
  try {
    otpRate.delete(email);
    if (redis) {
      try {
        await redis.del(`otp:${email}:count`);
      } catch (e) {
        console.error('redis del failed', e);
      }
    }
    console.log('admin reset otp metrics for', email);
    res.json({ ok: true });
  } catch (err) {
    console.error('admin otp reset error', err);
    res.status(500).json({ error: 'failed' });
  }
});

// ---------- verify otp { email, otp } ----------
app.post('/auth/verify-otp', async (req, res) => {
  const email = normEmail(req.body && req.body.email);
  const otp = req.body && req.body.otp;
  if (!email || !otp)
    return res
      .status(400)
      .json({ error: 'email and otp required' });
  if (!isValidEmail(email))
    return res.status(400).json({ error: 'invalid_email' });
  try {
    const r = await pool.query(
      'SELECT id FROM users WHERE email=$1',
      [email]
    );
    if (r.rowCount === 0)
      return res.status(404).json({ error: 'user not found' });
    const userId = r.rows[0].id;
    const or = await pool.query(
      'SELECT id,expires_at,used,attempts,code_hash FROM otp_codes WHERE user_id=$1 ORDER BY id DESC LIMIT 1',
      [userId]
    );
    if (or.rowCount === 0)
      return res.status(400).json({ error: 'otp_invalid' });
    const row = or.rows[0];
    if (row.used) return res.status(400).json({ error: 'code used' });
    if (row.attempts >= MAX_OTP_VERIFY_ATTEMPTS)
      return res
        .status(429)
        .json({ error: 'too_many_attempts' });
    if (new Date(row.expires_at) < new Date())
      return res.status(400).json({ error: 'code expired' });
    let ok = false;
    if (row.code_hash) {
      ok = bcrypt.compareSync(otp, row.code_hash);
    } else {
      ok = false;
    }
    if (!ok) {
      await pool.query(
        'UPDATE otp_codes SET attempts = attempts + 1 WHERE id=$1',
        [row.id]
      );
      return res.status(400).json({ error: 'otp_invalid' });
    }
    await pool.query(
      'UPDATE otp_codes SET used=true WHERE id=$1',
      [row.id]
    );
    await pool.query(
      'UPDATE users SET verified=true WHERE id=$1',
      [userId]
    );
    const uRow = (
      await pool.query(
        'SELECT id,username,role FROM users WHERE id=$1',
        [userId]
      )
    ).rows[0];
    const tokens = await issueTokens(uRow);
    res.json({
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      role: uRow.role,
    });
  } catch (err) {
    console.error('verify-otp error', err);
    res.status(500).json({ error: 'verify failed' });
  }
});

// ---------- reset password via OTP ----------
app.post('/auth/reset-password', async (req, res) => {
  const email = normEmail(req.body && req.body.email);
  const { otp, new_password } = req.body || {};
  if (!email || !otp || !new_password)
    return res.status(400).json({
      error: 'email_otp_and_new_password_required',
    });
  if (!isValidEmail(email))
    return res.status(400).json({ error: 'invalid_email' });
  if (isAllDigits(new_password))
    return res.status(400).json({ error: 'password_invalid' });
  if (String(new_password).length < 6)
    return res.status(400).json({ error: 'password_too_short' });
  try {
    const r = await pool.query(
      'SELECT id FROM users WHERE email=$1',
      [email]
    );
    if (r.rowCount === 0)
      return res.status(404).json({ error: 'user not found' });
    const userId = r.rows[0].id;
    const or = await pool.query(
      'SELECT id,expires_at,used,attempts,code_hash,code FROM otp_codes WHERE user_id=$1 ORDER BY id DESC LIMIT 1',
      [userId]
    );
    if (or.rowCount === 0)
      return res.status(400).json({ error: 'otp_invalid' });
    const row = or.rows[0];
    if (row.used) return res.status(400).json({ error: 'code used' });
    if (row.attempts >= MAX_OTP_VERIFY_ATTEMPTS)
      return res
        .status(429)
        .json({ error: 'too_many_attempts' });
    if (new Date(row.expires_at) < new Date())
      return res.status(400).json({ error: 'code expired' });
    let ok = false;
    if (row.code_hash) ok = bcrypt.compareSync(otp, row.code_hash);
    else if (row.code) ok = String(otp) === String(row.code);
    if (!ok) {
      await pool.query(
        'UPDATE otp_codes SET attempts = attempts + 1 WHERE id=$1',
        [row.id]
      );
      return res.status(400).json({ error: 'otp_invalid' });
    }
    await pool.query(
      'UPDATE otp_codes SET used=true WHERE id=$1',
      [row.id]
    );
    const hashed = bcrypt.hashSync(new_password, 10);
    await pool.query(
      'UPDATE users SET password=$1, verified=true WHERE id=$2',
      [hashed, userId]
    );
    return res.json({ ok: true });
  } catch (e) {
    console.error('reset-password error', e);
    return res.status(500).json({ error: 'failed' });
  }
});

// ---------- Admin-only: fetch last OTP for an email ----------
app.get('/admin/last-otp', requireAuth, async (req, res) => {
  if (!req.user || req.user.role !== 'ADMIN')
    return res.status(403).json({ error: 'forbidden' });
  const email = normEmail(req.query && req.query.email);
  if (!email)
    return res.status(400).json({ error: 'email required' });
  try {
    const ur = await pool.query(
      'SELECT id FROM users WHERE email=$1 LIMIT 1',
      [email]
    );
    if (ur.rowCount === 0)
      return res.status(404).json({ error: 'user not found' });
    const userId = ur.rows[0].id;
    const r = await pool.query(
      'SELECT code,code_hash,expires_at,used,attempts FROM otp_codes WHERE user_id=$1 ORDER BY id DESC LIMIT 1',
      [userId]
    );
    if (r.rowCount === 0)
      return res.status(404).json({ error: 'no_otp_found' });
    const row = r.rows[0];
    const codeToReturn =
      process.env.DEBUG_SHOW_OTP === 'true' ? row.code || null : null;
    return res.json({
      ok: true,
      code: codeToReturn,
      expires_at: row.expires_at,
      used: row.used,
      attempts: row.attempts,
    });
  } catch (e) {
    console.error('admin last-otp error', e);
    res.status(500).json({ error: 'failed' });
  }
});

// ---------- Admin-only: SMTP health-check ----------
app.get('/admin/smtp-check', requireAuth, async (req, res) => {
  if (!req.user || req.user.role !== 'ADMIN')
    return res.status(403).json({ error: 'forbidden' });
  if (!process.env.SMTP_HOST)
    return res
      .status(400)
      .json({ ok: false, note: 'SMTP not configured' });
  try {
    const nodemailer = require('nodemailer');
    const testTransport = nodemailer.createTransport({
      host: process.env.SMTP_HOST,
      port: parseInt(process.env.SMTP_PORT || '587', 10),
      secure: process.env.SMTP_SECURE === 'true',
      auth: process.env.SMTP_USER
        ? {
            user: process.env.SMTP_USER,
            pass: process.env.SMTP_PASS,
          }
        : undefined,
      tls: { rejectUnauthorized: false },
    });
    const info = await testTransport.verify();
    res.json({ ok: true, info });
  } catch (e) {
    console.error('smtp-check error', e);
    res.status(500).json({ ok: false, error: e.message || String(e) });
  }
});

// ---------- Auth: register email + password ----------
app.post('/auth/register', async (req, res) => {
  try {
    const { username, password, role, name } = req.body || {};
    const email = normEmail(username);

    if (!email || !password) {
      return res.status(400).json({ error: 'email_and_password_required' });
    }
    if (!isValidEmail(email)) {
      return res.status(400).json({ error: 'invalid_email' });
    }
    if (!isGmailAddress(email)) {
      return res.status(400).json({ error: 'only_gmail_allowed' });
    }
    if (String(password).length < 6) {
      return res.status(400).json({ error: 'password_too_short' });
    }

    // ✅ FIX: Map role từ frontend sang database role
    let finalRole = (role && String(role).toUpperCase()) || 'USER';
    
    // Map các role từ frontend sang database role
    if (finalRole === 'CUSTOMER') {
      finalRole = 'USER'; // Database dùng 'USER' thay vì 'CUSTOMER'
    } else if (finalRole === 'SHOP') {
      finalRole = 'MERCHANT'; // Database dùng 'MERCHANT' thay vì 'SHOP'
    }
    
    // Database chỉ cho phép: 'USER', 'ADMIN', 'SHOP', 'MERCHANT', 'SHIPPER'
    const allowedRoles = ['USER', 'ADMIN', 'SHOP', 'MERCHANT', 'SHIPPER'];
    if (!allowedRoles.includes(finalRole)) {
      console.warn(`[REGISTER] Invalid role "${finalRole}", defaulting to USER`);
      finalRole = 'USER';
    }

    const hashed = bcrypt.hashSync(password, 10);

    console.log(`[REGISTER] Attempting to register user: ${email}, role: ${finalRole}`);
    console.log(`[REGISTER] Database: ${DB_NAME}@${DB_HOST}:${DB_PORT}, User: ${DB_USER}`);
    
    // ✅ FIX: Kiểm tra xem user đã tồn tại chưa
    const existingUser = await pool.query('SELECT id, username, email, role, created_at FROM users WHERE username = $1 OR email = $1', [email]);
    if (existingUser.rows.length > 0) {
      console.log(`[REGISTER] ⚠️  User already exists:`, existingUser.rows[0]);
    }
    
    console.log(`[REGISTER] Executing INSERT query with params:`, {
      username: email,
      email: email,
      role: finalRole,
      verified: true
    });
    
    // ✅ FIX: Sử dụng client riêng để đảm bảo commit
    const client = await pool.connect();
    let user;
    try {
      await client.query('BEGIN');
      
      const r = await client.query(
        `INSERT INTO users (username, email, password, role, verified)
         VALUES ($1,$2,$3,$4,$5)
         ON CONFLICT (username)
         DO UPDATE SET
           email    = EXCLUDED.email,
           password = EXCLUDED.password,
           role     = EXCLUDED.role,
           verified = true
         RETURNING id, username, role, email, created_at`,
        [email, email, hashed, finalRole, true]
      );

      console.log(`[REGISTER] Query result:`, {
        rowCount: r.rowCount,
        rows: r.rows
      });

      if (r.rows.length === 0) {
        await client.query('ROLLBACK');
        console.error('[REGISTER] ❌ Failed to insert/update user, no rows returned');
        return res.status(500).json({ error: 'register_failed', message: 'Could not create user' });
      }

      user = r.rows[0];
      await client.query('COMMIT');
      console.log(`[REGISTER] ✅ Transaction committed successfully`);
    } catch (err) {
      await client.query('ROLLBACK');
      throw err;
    } finally {
      client.release();
    }

    console.log(`[REGISTER] ✅ User created/updated successfully:`);
    console.log(`[REGISTER]    - ID: ${user.id}`);
    console.log(`[REGISTER]    - Username: ${user.username}`);
    console.log(`[REGISTER]    - Email: ${user.email}`);
    console.log(`[REGISTER]    - Role: ${user.role}`);
    console.log(`[REGISTER]    - Created At: ${user.created_at}`);
    console.log(`[REGISTER]    - Database: ${DB_NAME}@${DB_HOST}:${DB_PORT}`);
    
    // ✅ FIX: Verify user was actually saved by querying again
    const verifyUser = await pool.query('SELECT id, username, email, role, created_at FROM users WHERE id = $1', [user.id]);
    if (verifyUser.rows.length > 0) {
      console.log(`[REGISTER] ✅ Verified user in database:`, verifyUser.rows[0]);
    } else {
      console.error(`[REGISTER] ❌ WARNING: User ${user.id} not found in database after insert!`);
    }
    
    const tokens = await issueTokens(user);

    return res.json({
      ok: true,
      user: {
        id: user.id,
        username: user.username,
        role: user.role,
        name: name || null,
      },
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
    });
  } catch (err) {
    console.error('[REGISTER] ❌ Error:', err);
    console.error('[REGISTER] Error details:', {
      message: err.message,
      code: err.code,
      detail: err.detail,
      hint: err.hint,
      constraint: err.constraint,
      table: err.table,
      column: err.column,
      stack: err.stack
    });
    return res.status(500).json({ 
      error: 'register_failed', 
      message: err.message,
      detail: err.detail,
      hint: err.hint
    });
  }
});

// ---------- Auth: login ----------
app.post('/auth/login', async (req, res) => {
  try {
    const { username, password } = req.body || {};
    const email = normEmail(username);

    console.log(`[LOGIN] Attempting login: username="${username}", normalized email="${email}"`);

    if (!email || !password) {
      console.log('[LOGIN] ❌ Missing email or password');
      return res.status(400).json({ error: 'email_and_password_required' });
    }
    if (!isValidEmail(email)) {
      console.log('[LOGIN] ❌ Invalid email format');
      return res.status(400).json({ error: 'invalid_email' });
    }

    const r = await pool.query(
      'SELECT id, username, password, role, verified FROM users WHERE email=$1 LIMIT 1',
      [email]
    );
    
    console.log(`[LOGIN] Query result: ${r.rowCount} user(s) found`);
    
    if (r.rowCount === 0) {
      console.log(`[LOGIN] ❌ User not found with email: ${email}`);
      return res.status(400).json({ error: 'invalid_credentials' });
    }

    const user = r.rows[0];
    console.log(`[LOGIN] User found: ID=${user.id}, Role=${user.role}, Verified=${user.verified}`);

    // ✅ FIX: Kiểm tra password null hoặc empty string
    if (!user.password || user.password.trim() === '') {
      console.log('[LOGIN] ❌ User has no password set');
      return res.status(400).json({ error: 'password_not_set' });
    }

    const ok = bcrypt.compareSync(password, user.password);
    console.log(`[LOGIN] Password match: ${ok}`);
    
    if (!ok) {
      console.log('[LOGIN] ❌ Password does not match');
      return res.status(400).json({ error: 'invalid_credentials' });
    }

    const tokens = await issueTokens(user);
    console.log(`[LOGIN] ✅ Login successful for user ID=${user.id}, Role=${user.role}`);

    return res.json({
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      role: user.role,
      verified: user.verified,
    });
  } catch (err) {
    console.error('[LOGIN] ❌ Error:', err);
    console.error('[LOGIN] Error details:', {
      message: err.message,
      stack: err.stack
    });
    return res.status(500).json({ error: 'login_failed' });
  }
});

// ---------- Auth: refresh ----------
app.post('/auth/refresh', async (req, res) => {
  try {
    const body = req.body || {};
    const rt =
      body.refresh_token ||
      body.refreshToken ||
      body.token;

    if (!rt) {
      return res.status(400).json({ error: 'refresh_token_required' });
    }

    let payload;
    try {
      payload = jwt.verify(rt, JWT_SECRET);
    } catch (e) {
      return res.status(401).json({ error: 'invalid_refresh_token' });
    }

    if (!payload || payload.type !== 'refresh' || !payload.id) {
      return res.status(401).json({ error: 'invalid_refresh_token' });
    }

    const r = await pool.query(
      'SELECT id, username, role FROM users WHERE id=$1 LIMIT 1',
      [payload.id]
    );
    if (r.rowCount === 0) {
      return res.status(404).json({ error: 'user_not_found' });
    }

    const user = r.rows[0];
    const tokens = await issueTokens(user);

    return res.json({
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      role: user.role,
    });
  } catch (err) {
    console.error('auth/refresh error', err);
    return res.status(500).json({ error: 'refresh_failed' });
  }
});

// ---------- Auth: logout ----------
app.post('/auth/logout', async (req, res) => {
  try {
    const auth = req.headers.authorization || '';
    const parts = auth.split(' ');
    let jti = null;

    if (parts.length === 2 && parts[0] === 'Bearer') {
      try {
        const payload = jwt.verify(parts[1], JWT_SECRET);
        if (payload && payload.jti) {
          jti = payload.jti;
        }
      } catch (e) {
        // token hết hạn / sai thì cũng coi như đã logout
      }
    }

    if (jti) {
      try {
        await pool.query(
          'INSERT INTO revoked_tokens (jti) VALUES ($1) ON CONFLICT (jti) DO NOTHING',
          [jti]
        );
      } catch (e) {
        console.error('logout: failed to insert revoked token', e);
      }
    }

    return res.json({ ok: true });
  } catch (err) {
    console.error('auth/logout error', err);
    return res.status(500).json({ error: 'logout_failed' });
  }
});

// ---------- Helper: auto-assign a shipper ----------
async function assignShipper(orderId) {
  const ord = await pool.query(
    'SELECT o.id,o.restaurant_id,o.user_id,r.lat AS rlat, r.lng AS rlng FROM orders o JOIN restaurants r ON r.id=o.restaurant_id WHERE o.id=$1',
    [orderId]
  );
  if (ord.rowCount === 0) return null;
  const { rlat, rlng } = ord.rows[0];
  if (rlat != null && rlng != null) {
    const s = await pool.query(
      'SELECT id,lat,lng FROM shippers WHERE available=true AND lat IS NOT NULL AND lng IS NOT NULL'
    );
    if (s.rowCount > 0) {
      let best = null;
      let bestDist = Infinity;
      for (const row of s.rows) {
        const dlat = parseFloat(row.lat) - parseFloat(rlat);
        const dlng = parseFloat(row.lng) - parseFloat(rlng);
        const dist = dlat * dlat + dlng * dlng;
        const geoDist = Math.sqrt(dist);
        if (geoDist < bestDist) {
          bestDist = geoDist;
          best = row;
        }
      }
      if (best) {
        const shipperId = best.id;
        await pool.query(
          'UPDATE orders SET shipper_id=$1, status=$2 WHERE id=$3',
          [shipperId, 'PICKED_UP', orderId]
        );
        await pool.query(
          'INSERT INTO order_status_history(order_id,status,note) VALUES($1,$2,$3)',
          [orderId, 'PICKED_UP', 'Assigned to shipper']
        );
        await pool.query(
          'UPDATE shippers SET available=false WHERE id=$1',
          [shipperId]
        );
        io.to(`order_${orderId}`).emit('statusUpdate', {
          status: 'PICKED_UP',
          shipperId,
        });
        io.to(`user_${ord.rows[0].user_id}`).emit('orderUpdate', {
          orderId,
          status: 'PICKED_UP',
        });
        return shipperId;
      }
    }
  }
  const r = await pool.query(
    'SELECT id FROM shippers WHERE available=true LIMIT 1'
  );
  if (r.rowCount === 0) return null;
  const shipperId = r.rows[0].id;
  await pool.query(
    'UPDATE orders SET shipper_id=$1, status=$2 WHERE id=$3',
    [shipperId, 'PICKED_UP', orderId]
  );
  await pool.query(
    'INSERT INTO order_status_history(order_id,status,note) VALUES($1,$2,$3)',
    [orderId, 'PICKED_UP', 'Assigned to shipper (fallback)']
  );
  await pool.query('UPDATE shippers SET available=false WHERE id=$1', [
    shipperId,
  ]);
  io.to(`order_${orderId}`).emit('statusUpdate', {
    status: 'PICKED_UP',
    shipperId,
  });
  io.to(`user_${ord.rows[0].user_id}`).emit('orderUpdate', {
    orderId,
    status: 'PICKED_UP',
  });
  return shipperId;
}

// ---------- Allowed transitions per role ----------
const ALLOWED_TRANSITIONS = {
  USER: [], // user triggers create/cancel only via endpoints
  SHOP: ['CONFIRMED', 'COOKING', 'READY', 'HANDOVER'],
  SHIPPER: ['PICKED_UP', 'DELIVERING', 'DELIVERED'],
  ADMIN: ['CANCELED', 'CONFIRMED', 'PICKED_UP', 'DELIVERING', 'DELIVERED'],
};

function canChangeStatus(role, newStatus) {
  if (!role) return false;
  if (role === 'ADMIN') return true;
  const allowed = ALLOWED_TRANSITIONS[role];
  if (!allowed) return false;
  return allowed.includes(newStatus);
}

// ---------- socket.io ----------
io.on('connection', (socket) => {
  console.log('socket connected', socket.id);
  socket.on('joinOrder', (orderId) => {
    socket.join(`order_${orderId}`);
  });
  socket.on('leaveOrder', (orderId) => {
    socket.leave(`order_${orderId}`);
  });
  socket.on('identify', (payload) => {
    if (payload && payload.userId) socket.join(`user_${payload.userId}`);
    if (payload && payload.role && payload.role === 'ADMIN')
      socket.join('admins');
  });
});

// ---------- Admin Orders ----------
app.get('/admin/orders', requireAuth, async (req, res) => {
  if (!req.user || req.user.role !== 'ADMIN')
    return res.status(403).json({ error: 'forbidden' });
  const { status } = req.query;
  try {
    let query = `
      SELECT 
        o.id,
        o.code,
        o.user_id,
        o.restaurant_id,
        o.shipper_id,
        COALESCE(o.total_amount, o.total, 0) as total,
        COALESCE(o.total_amount, o.total, 0) as total_amount,
        o.status,
        o.address,
        o.payment_method,
        o.created_at,
        o.updated_at,
        COALESCE(u.name, u.username, u.email, 'N/A') as customer_name,
        u.email as customer_email,
        r.name as restaurant_name
      FROM orders o
      LEFT JOIN users u ON u.id = o.user_id
      LEFT JOIN restaurants r ON r.id = o.restaurant_id
      WHERE 1=1
    `;
    const params = [];
    
    if (status) {
      const statusUpper = status.toUpperCase();
      // Support multiple statuses for admin
      if (statusUpper === 'PENDING' || statusUpper === 'NEW') {
        query += ' AND (o.status = $1 OR o.status = $2)';
        params.push('PENDING', 'CONFIRMED');
      } else if (statusUpper === 'DELIVERED' || statusUpper === 'COMPLETED') {
        query += ' AND o.status = $1';
        params.push('DELIVERED');
      } else if (statusUpper === 'CANCELLED' || statusUpper === 'CANCELED') {
        query += ' AND o.status = $1';
        params.push('CANCELED');
      } else {
        // Use status directly
        query += ' AND o.status = $1';
        params.push(statusUpper);
      }
    }
    
    query += ' ORDER BY o.created_at DESC';
    
    const result = await pool.query(query, params);
    res.json({ orders: result.rows });
  } catch (err) {
    console.error('admin orders error', err);
    res.status(500).json({ error: 'failed' });
  }
});

app.get('/admin/stats', requireAuth, async (req, res) => {
  if (!req.user || req.user.role !== 'ADMIN')
    return res.status(403).json({ error: 'forbidden' });
  try {
    const totalOrders = (
      await pool.query('SELECT COUNT(*) FROM orders')
    ).rows[0].count;
    const pending = (
      await pool.query("SELECT COUNT(*) FROM orders WHERE status='PENDING'")
    ).rows[0].count;
    const delivering = (
      await pool.query(
        "SELECT COUNT(*) FROM orders WHERE status='DELIVERING'"
      )
    ).rows[0].count;
    res.json({
      totalOrders: Number(totalOrders),
      pending: Number(pending),
      delivering: Number(delivering),
    });
  } catch (err) {
    console.error('admin stats error', err);
    res.status(500).json({ error: 'failed' });
  }
});

// Thống kê chi tiết cho admin dashboard
app.get('/admin/stats/detailed', requireAuth, async (req, res) => {
  if (!req.user || req.user.role !== 'ADMIN')
    return res.status(403).json({ error: 'forbidden' });
  try {
    const stats = {
      orders: {},
      revenue: {},
      users: {},
      ratings: {}
    };

    // Thống kê đơn hàng theo trạng thái
    const orderStats = await pool.query(`
      SELECT status, COUNT(*) as count 
      FROM orders 
      GROUP BY status
    `);
    orderStats.rows.forEach(row => {
      stats.orders[row.status.toLowerCase()] = Number(row.count);
    });

    // Tổng số đơn hàng
    const totalOrders = await pool.query('SELECT COUNT(*) as count FROM orders');
    stats.orders.total = Number(totalOrders.rows[0].count);

    // Doanh thu (tổng từ các đơn đã giao)
    const revenue = await pool.query(`
      SELECT 
        COALESCE(SUM(total), 0) as total_revenue,
        COUNT(*) as completed_orders
      FROM orders 
      WHERE status = 'DELIVERED'
    `);
    stats.revenue.total = Number(revenue.rows[0].total_revenue) || 0;
    stats.revenue.completedOrders = Number(revenue.rows[0].completed_orders) || 0;

    // Doanh thu hôm nay
    const todayRevenue = await pool.query(`
      SELECT 
        COALESCE(SUM(total), 0) as today_revenue,
        COUNT(*) as today_orders
      FROM orders 
      WHERE status = 'DELIVERED' 
        AND DATE(created_at) = CURRENT_DATE
    `);
    stats.revenue.today = Number(todayRevenue.rows[0].today_revenue) || 0;
    stats.revenue.todayOrders = Number(todayRevenue.rows[0].today_orders) || 0;

    // Thống kê người dùng
    const userStats = await pool.query(`
      SELECT role, COUNT(*) as count 
      FROM users 
      GROUP BY role
    `);
    userStats.rows.forEach(row => {
      stats.users[row.role.toLowerCase()] = Number(row.count);
    });
    const totalUsers = await pool.query('SELECT COUNT(*) as count FROM users');
    stats.users.total = Number(totalUsers.rows[0].count);

    // Số yêu cầu hủy đơn đang chờ
    const cancelRequests = await pool.query(`
      SELECT COUNT(*) as count 
      FROM orders 
      WHERE status = 'CANCEL_REQUESTED'
    `);
    stats.orders.cancelRequests = Number(cancelRequests.rows[0].count) || 0;

    res.json(stats);
  } catch (err) {
    console.error('admin detailed stats error', err);
    res.status(500).json({ error: 'failed' });
  }
});

// Lấy danh sách yêu cầu hủy đơn
app.get('/admin/cancel-requests', requireAuth, async (req, res) => {
  if (!req.user || req.user.role !== 'ADMIN')
    return res.status(403).json({ error: 'forbidden' });
  try {
    const result = await pool.query(`
      SELECT 
        o.id, 
        o.code,
        o.user_id,
        o.restaurant_id,
        o.total,
        o.status,
        o.address as delivery_address,
        o.created_at,
        u.email as customer_email,
        COALESCE(u.name, u.username, u.email) as customer_name,
        r.name as restaurant_name,
        (
          SELECT note 
          FROM order_status_history 
          WHERE order_id = o.id 
            AND status = 'CANCEL_REQUESTED' 
          ORDER BY created_at DESC 
          LIMIT 1
        ) as cancel_reason
      FROM orders o
      LEFT JOIN users u ON o.user_id = u.id
      LEFT JOIN restaurants r ON o.restaurant_id = r.id
      WHERE o.status = 'CANCEL_REQUESTED'
      ORDER BY o.created_at DESC
    `);
    res.json({ orders: result.rows });
  } catch (err) {
    console.error('admin cancel requests error', err);
    res.status(500).json({ error: 'failed' });
  }
});

// Duyệt hủy đơn hàng
app.post('/admin/orders/:id/approve-cancel', requireAuth, async (req, res) => {
  if (!req.user || req.user.role !== 'ADMIN')
    return res.status(403).json({ error: 'forbidden' });
  
  const orderId = parseInt(req.params.id, 10);
  if (isNaN(orderId)) {
    return res.status(400).json({ error: 'invalid_order_id' });
  }

  try {
    const client = await pool.connect();
    try {
      await client.query('BEGIN');

      // Kiểm tra đơn hàng
      const orderResult = await client.query(
        'SELECT id, user_id, status, total, payment_method FROM orders WHERE id = $1',
        [orderId]
      );

      if (orderResult.rows.length === 0) {
        await client.query('ROLLBACK');
        return res.status(404).json({ error: 'order_not_found' });
      }

      const order = orderResult.rows[0];
      
      if (order.status !== 'CANCEL_REQUESTED') {
        await client.query('ROLLBACK');
        return res.status(400).json({ 
          error: 'invalid_status', 
          message: 'Đơn hàng không ở trạng thái yêu cầu hủy' 
        });
      }

      // Cập nhật trạng thái thành CANCELED
      await client.query(
        'UPDATE orders SET status = $1, updated_at = NOW() WHERE id = $2',
        ['CANCELED', orderId]
      );

      // Thêm vào lịch sử
      await client.query(
        'INSERT INTO order_status_history (order_id, status, note) VALUES ($1, $2, $3)',
        [orderId, 'CANCELED', `Đơn hàng đã được admin duyệt hủy. Hoàn tiền nếu cần.`]
      );

      // TODO: Xử lý refund nếu đã thanh toán (cần thêm logic refund)
      // if (order.payment_method !== 'CASH' && order.total > 0) {
      //   // Process refund
      // }

      await client.query('COMMIT');

      // Emit socket event
      io.to(`user_${order.user_id}`).emit('orderUpdate', {
        orderId,
        status: 'CANCELED',
      });
      io.to('admins').emit('cancelRequestApproved', { orderId });

      res.json({
        success: true,
        orderId,
        status: 'CANCELED',
        message: 'Đã duyệt hủy đơn hàng thành công'
      });
    } catch (err) {
      await client.query('ROLLBACK');
      throw err;
    } finally {
      client.release();
    }
  } catch (err) {
    console.error('approve cancel error', err);
    res.status(500).json({ error: 'failed', message: err.message });
  }
});

// Từ chối yêu cầu hủy đơn
app.post('/admin/orders/:id/reject-cancel', requireAuth, async (req, res) => {
  if (!req.user || req.user.role !== 'ADMIN')
    return res.status(403).json({ error: 'forbidden' });
  
  const orderId = parseInt(req.params.id, 10);
  const { reason } = req.body || {};
  
  if (isNaN(orderId)) {
    return res.status(400).json({ error: 'invalid_order_id' });
  }

  try {
    // Lấy trạng thái trước đó từ lịch sử (CONFIRMED, COOKING, hoặc READY)
    const historyResult = await pool.query(`
      SELECT status 
      FROM order_status_history 
      WHERE order_id = $1 
        AND status != 'CANCEL_REQUESTED'
      ORDER BY created_at DESC 
      LIMIT 1
    `, [orderId]);

    const previousStatus = historyResult.rows[0]?.status || 'CONFIRMED';

    // Cập nhật lại trạng thái về trạng thái trước đó
    await pool.query(
      'UPDATE orders SET status = $1, updated_at = NOW() WHERE id = $2 AND status = $3',
      [previousStatus, orderId, 'CANCEL_REQUESTED']
    );

    // Thêm vào lịch sử
    await pool.query(
      'INSERT INTO order_status_history (order_id, status, note) VALUES ($1, $2, $3)',
      [orderId, previousStatus, `Admin từ chối yêu cầu hủy đơn. ${reason || ''}`]
    );

    const orderResult = await pool.query('SELECT user_id FROM orders WHERE id = $1', [orderId]);
    const userId = orderResult.rows[0]?.user_id;

    if (userId) {
      io.to(`user_${userId}`).emit('orderUpdate', {
        orderId,
        status: previousStatus,
      });
    }

    res.json({
      success: true,
      orderId,
      status: previousStatus,
      message: 'Đã từ chối yêu cầu hủy đơn'
    });
  } catch (err) {
    console.error('reject cancel error', err);
    res.status(500).json({ error: 'failed', message: err.message });
  }
});

// Lấy danh sách người dùng
app.get('/admin/users', requireAuth, async (req, res) => {
  if (!req.user || req.user.role !== 'ADMIN')
    return res.status(403).json({ error: 'forbidden' });
  
  const { role, search } = req.query;
  
  try {
    // Đảm bảo cột is_active tồn tại
    try {
      await pool.query('ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true');
    } catch (alterErr) {
      console.log('[ADMIN USERS] is_active column check:', alterErr.message);
    }

    let query = `
      SELECT 
        id,
        email,
        COALESCE(name, username, email) as name,
        phone,
        role,
        COALESCE(is_active, true) as is_active,
        created_at
      FROM users
      WHERE 1=1
    `;
    const params = [];
    let paramIndex = 1;

    if (role) {
      query += ` AND role = $${paramIndex}`;
      params.push(role.toUpperCase());
      paramIndex++;
    }

    if (search) {
      query += ` AND (email ILIKE $${paramIndex} OR name ILIKE $${paramIndex} OR username ILIKE $${paramIndex} OR phone ILIKE $${paramIndex})`;
      params.push(`%${search}%`);
      paramIndex++;
    }

    query += ' ORDER BY created_at DESC';

    const result = await pool.query(query, params);
    res.json({ users: result.rows });
  } catch (err) {
    console.error('admin users error', err);
    res.status(500).json({ error: 'failed' });
  }
});

// Cập nhật trạng thái người dùng (khóa/mở khóa)
app.patch('/admin/users/:id/status', requireAuth, async (req, res) => {
  if (!req.user || req.user.role !== 'ADMIN')
    return res.status(403).json({ error: 'forbidden' });
  
  const userId = parseInt(req.params.id, 10);
  const { is_active } = req.body;
  
  if (isNaN(userId)) {
    return res.status(400).json({ error: 'invalid_user_id' });
  }

  if (typeof is_active !== 'boolean') {
    return res.status(400).json({ error: 'is_active_required' });
  }

  try {
    // Đảm bảo cột is_active tồn tại
    try {
      await pool.query('ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true');
    } catch (alterErr) {
      console.log('[ADMIN USERS STATUS] is_active column check:', alterErr.message);
    }

    // Không cho khóa chính mình
    if (userId === req.user.id) {
      return res.status(400).json({ error: 'cannot_disable_self' });
    }

    await pool.query(
      'UPDATE users SET is_active = $1 WHERE id = $2',
      [is_active, userId]
    );

    res.json({
      success: true,
      userId,
      is_active,
      message: is_active ? 'Đã kích hoạt tài khoản' : 'Đã khóa tài khoản'
    });
  } catch (err) {
    console.error('update user status error', err);
    res.status(500).json({ error: 'failed' });
  }
});

/* ===========================
 *   RESTAURANT ENDPOINTS
 * =========================== */

// Lấy danh sách tất cả restaurants
app.get('/api/v1/restaurants', async (req, res) => {
  try {
    const r = await pool.query(
      'SELECT id, name, address, rating, image_url, lat, lng FROM restaurants ORDER BY id'
    );
    return res.json(r.rows);
  } catch (e) {
    console.error('GET /api/v1/restaurants error', e);
    return res.status(500).json({ error: 'failed_to_fetch_restaurants' });
  }
});

// Lấy danh sách restaurants gần nhất (với location)
app.get('/api/v1/restaurants/nearby', async (req, res) => {
  try {
    const lat = parseFloat(req.query.lat);
    const lng = parseFloat(req.query.lng);

    if (isNaN(lat) || isNaN(lng)) {
      return res.status(400).json({ error: 'invalid_location', message: 'lat and lng are required' });
    }

    // Tính khoảng cách bằng công thức Haversine (đơn giản hóa)
    // distance = sqrt((lat - r.lat)^2 + (lng - r.lng)^2) * 111 (km per degree)
    // ✅ FIX: Chỉ hiển thị restaurants trong vòng 15km
    const r = await pool.query(
      `SELECT 
        id, 
        name, 
        address, 
        rating, 
        image_url, 
        lat, 
        lng,
        CASE 
          WHEN lat IS NOT NULL AND lng IS NOT NULL 
          THEN SQRT(POWER($1 - lat, 2) + POWER($2 - lng, 2)) * 111.0
          ELSE NULL 
        END AS distance
      FROM restaurants
      WHERE lat IS NOT NULL 
        AND lng IS NOT NULL
        AND SQRT(POWER($1 - lat, 2) + POWER($2 - lng, 2)) * 111.0 <= 15.0
      ORDER BY distance ASC
      LIMIT 50`,
      [lat, lng]
    );

    return res.json(r.rows);
  } catch (e) {
    console.error('GET /api/v1/restaurants/nearby error', e);
    return res.status(500).json({ error: 'failed_to_fetch_nearby_restaurants' });
  }
});

// Lấy chi tiết 1 restaurant
app.get('/api/v1/restaurants/:id', async (req, res) => {
  const id = parseInt(req.params.id, 10);
  if (Number.isNaN(id)) {
    return res.status(400).json({ error: 'invalid_restaurant_id' });
  }
  try {
    const r = await pool.query(
      'SELECT id, name, address, rating, image_url, lat, lng FROM restaurants WHERE id = $1',
      [id]
    );
    if (r.rowCount === 0) {
      return res.status(404).json({ error: 'restaurant_not_found' });
    }
    return res.json(r.rows[0]);
  } catch (e) {
    console.error('GET /api/v1/restaurants/:id error', e);
    return res.status(500).json({ error: 'failed_to_fetch_restaurant_detail' });
  }
});

// Lấy menu (products) của 1 restaurant
app.get('/api/v1/restaurants/:id/menu', async (req, res) => {
  const id = parseInt(req.params.id, 10);
  if (Number.isNaN(id)) {
    return res.status(400).json({ error: 'invalid_restaurant_id' });
  }
  try {
    const r = await pool.query(
      'SELECT id, restaurant_id, name, description, price, image_url FROM products WHERE restaurant_id = $1 ORDER BY id',
      [id]
    );
    return res.json(r.rows);
  } catch (e) {
    console.error('GET /api/v1/restaurants/:id/menu error', e);
    return res.status(500).json({ error: 'failed_to_fetch_menu' });
  }
});

/* ===========================
 *          ORDERS
 * =========================== */

app.post('/orders', requireAuth, async (req, res) => {
  const { restaurant_id, items, address, payment_method } = req.body;
  const user_id = req.user.id;

  if (
    !restaurant_id ||
    !items ||
    !Array.isArray(items) ||
    items.length === 0 ||
    !address ||
    !payment_method
  ) {
    return res
      .status(400)
      .json({ error: 'missing_required_fields_or_empty_items' });
  }

  const client = await pool.connect();

  try {
    await client.query('BEGIN');

    const itemIds = items.map((i) => i.product_id);

    const pricesResult = await client.query(
      'SELECT id, price FROM products WHERE id = ANY($1::int[])',
      [itemIds]
    );

    const priceMap = new Map();
    pricesResult.rows.forEach((row) =>
      priceMap.set(row.id, parseFloat(row.price))
    );

    let totalAmount = 0;
    const orderItemsData = [];

    for (const item of items) {
      const productId = parseInt(item.product_id, 10);
      const quantity = parseInt(item.quantity, 10);

      if (isNaN(productId) || isNaN(quantity) || quantity <= 0) {
        await client.query('ROLLBACK');
        return res
          .status(400)
          .json({ error: 'invalid_item_structure_or_quantity' });
      }

      const price = priceMap.get(productId);

      if (price === undefined) {
        await client.query('ROLLBACK');
        return res
          .status(404)
          .json({ error: `product_not_found: ${productId}` });
      }

      totalAmount += price * quantity;
      orderItemsData.push({ productId, quantity, price });
    }

    if (totalAmount <= 0) {
      await client.query('ROLLBACK');
      return res
        .status(400)
        .json({ error: 'order_total_must_be_positive' });
    }

    // ✅ FIX: Lưu vào address (database chỉ có cột này, không có delivery_address)
    const orderInsert = await client.query(
      'INSERT INTO orders (user_id, restaurant_id, address, payment_method, total, status) VALUES ($1, $2, $3, $4, $5, $6) RETURNING id, status, created_at',
      [user_id, restaurant_id, address, payment_method, totalAmount, 'PENDING']
    );
    const orderId = orderInsert.rows[0].id;
    const newOrder = orderInsert.rows[0];

    // ✅ FIX: Kiểm tra schema của order_items và insert phù hợp
    // Lấy tên sản phẩm để lưu vào item_name (nếu schema yêu cầu)
    const productNames = new Map();
    for (const item of orderItemsData) {
      const productInfo = await client.query('SELECT name FROM products WHERE id = $1', [item.productId]);
      if (productInfo.rows.length > 0) {
        productNames.set(item.productId, productInfo.rows[0].name);
      }
    }

    const itemPromises = orderItemsData.map(async (item) => {
      // Thử với product_id trước (schema mới)
      try {
        await client.query(
          'INSERT INTO order_items (order_id, product_id, quantity, price) VALUES ($1, $2, $3, $4)',
          [orderId, item.productId, item.quantity, item.price]
        );
      } catch (err) {
        // Nếu lỗi vì không có cột product_id, thử với menu_item_id (schema cũ)
        if (err.message && (err.message.includes('column "product_id"') || err.message.includes('does not exist'))) {
          const productName = productNames.get(item.productId) || 'Unknown Product';
          await client.query(
            'INSERT INTO order_items (order_id, menu_item_id, item_name, quantity, unit_price) VALUES ($1, $2, $3, $4, $5)',
            [orderId, item.productId, productName, item.quantity, item.price]
          );
        } else {
          throw err;
        }
      }
    });
    await Promise.all(itemPromises);

    await client.query(
      'INSERT INTO order_status_history (order_id, status, note) VALUES ($1, $2, $3)',
      [orderId, 'PENDING', 'Order placed successfully']
    );

    await client.query('COMMIT');

    const notificationPayload = {
      orderId,
      userId: user_id,
      restaurantId: restaurant_id,
      status: 'PENDING',
      totalAmount,
    };

    io.to(`user_${user_id}`).emit('orderUpdate', notificationPayload);
    io.to(`shop_${restaurant_id}`).emit('newOrder', notificationPayload);
    io.to('admins').emit('newOrderAlert', notificationPayload);

    res.status(201).json({
      orderId: orderId,
      status: newOrder.status,
      totalAmount: totalAmount,
      created_at: newOrder.created_at,
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('order creation error', err);
    res.status(500).json({ error: 'failed_to_create_order' });
  } finally {
    client.release();
  }
});

// ---------- Get Order Detail ----------
app.get('/orders/:id', requireAuth, async (req, res) => {
  const orderIdParam = req.params.id;
  const user_id = req.user.id;
  const user_role = req.user.role;

  try {
    // ✅ FIX: Parse orderId sang integer
    const orderId = parseInt(orderIdParam, 10);
    if (isNaN(orderId)) {
      return res.status(400).json({ error: 'invalid_order_id' });
    }

    // Kiểm tra quyền: customer chỉ xem đơn của mình, admin/shop có thể xem tất cả
    // ✅ FIX: Thêm thông tin shipper vào query
    let query = `
      SELECT 
        o.id as order_id,
        o.user_id,
        o.restaurant_id,
        o.status,
        COALESCE(o.total_amount, o.total, 0) as total_amount,
        o.total,
        o.created_at,
        o.address,
        o.payment_method,
        o.shipper_id,
        r.name as restaurant_name,
        s.lat as shipper_lat,
        s.lng as shipper_lng,
        s.vehicle_plate as shipper_vehicle_plate,
        u_shipper.username as shipper_name,
        u_shipper.email as shipper_email,
        u_shipper.phone as shipper_phone
      FROM orders o
      LEFT JOIN restaurants r ON r.id = o.restaurant_id
      LEFT JOIN shippers s ON s.id = o.shipper_id
      LEFT JOIN users u_shipper ON u_shipper.id = o.shipper_id
      WHERE o.id = $1
    `;
    const params = [orderId];

    if (user_role !== 'ADMIN' && user_role !== 'MERCHANT' && user_role !== 'SHIPPER') {
      // Customer chỉ xem đơn của mình
      query += ' AND o.user_id = $2';
      params.push(user_id);
    } else if (user_role === 'MERCHANT') {
      // Merchant chỉ xem đơn của restaurant của họ
      // Tìm restaurant_id của merchant
      let restaurantId;
      let restaurantResult = await pool.query(
        'SELECT id FROM restaurants WHERE id = $1',
        [user_id]
      );
      
      if (restaurantResult.rows.length === 0) {
        try {
          restaurantResult = await pool.query(
            'SELECT restaurant_id as id FROM user_restaurants WHERE user_id = $1',
            [user_id]
          );
        } catch (e) {
          console.log('user_restaurants table may not exist');
        }
      }
      
      if (restaurantResult.rows.length === 0) {
        // Fallback: giả sử restaurant_id = user_id
        restaurantId = user_id;
      } else {
        restaurantId = restaurantResult.rows[0].id;
      }
      
      query += ' AND o.restaurant_id = $2';
      params.push(restaurantId);
    } else if (user_role === 'SHIPPER') {
      // Shipper chỉ xem đơn đã được gán cho họ hoặc đơn available
      query += ' AND (o.shipper_id = $2 OR o.shipper_id IS NULL)';
      params.push(user_id);
    }

    const result = await pool.query(query, params);

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'order_not_found' });
    }

    const order = result.rows[0];

    // Lấy items của đơn hàng
    // ✅ FIX: Thử cả hai schema (có thể database dùng product_id hoặc menu_item_id)
    let itemsResult;
    try {
      // Thử với schema mới (product_id, price)
      itemsResult = await pool.query(
        `SELECT 
          oi.product_id,
          oi.quantity,
          oi.price,
          p.name as product_name
        FROM order_items oi
        LEFT JOIN products p ON p.id = oi.product_id
        WHERE oi.order_id = $1`,
        [orderId]
      );
    } catch (err) {
      // Nếu lỗi, thử với schema cũ (menu_item_id, unit_price)
      console.log('Trying alternative schema...', err.message);
      itemsResult = await pool.query(
        `SELECT 
          oi.menu_item_id as product_id,
          oi.quantity,
          oi.unit_price as price,
          oi.item_name as product_name,
          p.name as product_name_from_menu
        FROM order_items oi
        LEFT JOIN menu_items p ON p.id = oi.menu_item_id
        WHERE oi.order_id = $1`,
        [orderId]
      );
    }
    
    // Map lại để đảm bảo format đúng
    const mappedItems = itemsResult.rows.map(item => {
      const unitPrice = parseFloat(item.price) || 0;
      const qty = parseInt(item.quantity) || 1;
      const lineTotal = unitPrice * qty;
      
      return {
        product_id: item.product_id,
        quantity: qty,
        price: unitPrice, // Giá đơn vị
        line_total: lineTotal, // ✅ Tổng giá = giá đơn vị * số lượng
        product_name: item.product_name_from_menu || item.product_name || 'Unknown'
      };
    });

    // Lấy lịch sử trạng thái
    // ✅ FIX: Bảng order_status_history có thể không có created_at, dùng id để sort
    let statusHistoryResult;
    try {
      statusHistoryResult = await pool.query(
        `SELECT status, note, created_at, id
        FROM order_status_history
        WHERE order_id = $1
        ORDER BY created_at ASC`,
        [orderId]
      );
    } catch (err) {
      // Nếu không có created_at, dùng id để sort
      statusHistoryResult = await pool.query(
        `SELECT status, note, id
        FROM order_status_history
        WHERE order_id = $1
        ORDER BY id ASC`,
        [orderId]
      );
    }

    // ✅ FIX: Luôn tính lại total từ items để đảm bảo chính xác (dùng line_total đã tính sẵn)
    let finalTotal = 0;
    if (mappedItems.length > 0) {
      finalTotal = mappedItems.reduce((sum, item) => {
        // Ưu tiên dùng line_total (đã tính sẵn), nếu không có thì tính lại
        const lineTotal = parseFloat(item.line_total) || (parseFloat(item.price) * parseInt(item.quantity) || 0);
        return sum + lineTotal;
      }, 0);
      const dbTotal = parseFloat(order.total_amount) || parseFloat(order.total) || 0;
      if (Math.abs(finalTotal - dbTotal) > 0.01) {
        console.log(`[GET ORDER] Calculated total from items: ${finalTotal} (DB had: ${dbTotal})`);
      }
    } else {
      finalTotal = parseFloat(order.total_amount) || parseFloat(order.total) || 0;
    }
    
    // ✅ FIX: Trả về đúng format mà OrderDetailFragment expect, kèm thông tin shipper
    const shipperInfo = order.shipper_id ? {
      shipper_id: order.shipper_id,
      shipper_name: order.shipper_name || order.shipper_email || 'N/A',
      shipper_email: order.shipper_email || null,
      shipper_phone: order.shipper_phone || null,
      vehicle_plate: order.shipper_vehicle_plate || null,
      shipper_lat: order.shipper_lat ? parseFloat(order.shipper_lat) : null,
      shipper_lng: order.shipper_lng ? parseFloat(order.shipper_lng) : null,
    } : null;
    
    res.json({
      order: {
        order_id: order.order_id,
        status: order.status,
        total: finalTotal,
        total_amount: finalTotal,
        address: order.address,
        payment_method: order.payment_method,
        created_at: order.created_at,
        restaurant_name: order.restaurant_name,
        restaurant_id: order.restaurant_id,
        user_id: order.user_id,
        shipper: shipperInfo,
      },
      items: mappedItems,
      history: statusHistoryResult.rows,
    });
  } catch (err) {
    console.error('get order detail error', err);
    console.error('Error stack:', err.stack);
    res.status(500).json({ error: 'failed_to_fetch_order', message: err.message });
  }
});

// ---------- Customer Orders ----------
app.get('/customer/orders', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const status = req.query.status; // Optional filter
  const page = parseInt(req.query.page) || 1;
  const pageSize = parseInt(req.query.page_size) || 20;
  const offset = (page - 1) * pageSize;

  try {
    let query = `
      SELECT 
        o.id as order_id,
        o.status,
        o.total_amount,
        o.created_at,
        o.address,
        o.payment_method,
        r.name as restaurant_name
      FROM orders o
      LEFT JOIN restaurants r ON r.id = o.restaurant_id
      WHERE o.user_id = $1
    `;
    const params = [user_id];

    if (status) {
      query += ' AND o.status = $2';
      params.push(status.toUpperCase());
    }

    query += ' ORDER BY o.created_at DESC LIMIT $' + (params.length + 1) + ' OFFSET $' + (params.length + 2);
    params.push(pageSize, offset);

    const result = await pool.query(query, params);

    // Lấy items cho mỗi đơn hàng
    const ordersWithItems = await Promise.all(
      result.rows.map(async (order) => {
        let itemsResult;
        try {
          // Thử với schema mới (product_id, price)
          itemsResult = await pool.query(
            `SELECT 
              oi.product_id,
              oi.quantity,
              oi.price,
              p.name as product_name
            FROM order_items oi
            LEFT JOIN products p ON p.id = oi.product_id
            WHERE oi.order_id = $1`,
            [order.order_id]
          );
        } catch (err) {
          // Nếu lỗi, thử với schema cũ (menu_item_id, unit_price)
          itemsResult = await pool.query(
            `SELECT 
              oi.menu_item_id as product_id,
              oi.quantity,
              oi.unit_price as price,
              oi.item_name as product_name,
              p.name as product_name_from_menu
            FROM order_items oi
            LEFT JOIN menu_items p ON p.id = oi.menu_item_id
            WHERE oi.order_id = $1`,
            [order.order_id]
          );
        }
        
        // Map lại để đảm bảo format đúng
        const mappedItems = itemsResult.rows.map(item => {
          const unitPrice = parseFloat(item.price) || 0;
          const qty = parseInt(item.quantity) || 1;
          const lineTotal = unitPrice * qty;
          
          return {
            product_id: item.product_id,
            quantity: qty,
            price: unitPrice,
            line_total: lineTotal,
            product_name: item.product_name_from_menu || item.product_name || 'Unknown'
          };
        });

        return {
          ...order,
          items: mappedItems,
          total: parseFloat(order.total_amount) || 0,
        };
      })
    );

    res.json(ordersWithItems);
  } catch (err) {
    console.error('customer orders error', err);
    res.status(500).json({ error: 'failed_to_fetch_orders' });
  }
});

app.get('/customer/recent-orders', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const limit = parseInt(req.query.limit) || 10;

  try {
    const result = await pool.query(
      `SELECT 
        o.id as order_id,
        o.status,
        o.total_amount,
        o.created_at,
        o.address,
        o.payment_method,
        r.name as restaurant_name
      FROM orders o
      LEFT JOIN restaurants r ON r.id = o.restaurant_id
      WHERE o.user_id = $1
      ORDER BY o.created_at DESC
      LIMIT $2`,
      [user_id, limit]
    );

    // Lấy items cho mỗi đơn hàng
    const ordersWithItems = await Promise.all(
      result.rows.map(async (order) => {
        let itemsResult;
        try {
          // Thử với schema mới (product_id, price)
          itemsResult = await pool.query(
            `SELECT 
              oi.product_id,
              oi.quantity,
              oi.price,
              p.name as product_name
            FROM order_items oi
            LEFT JOIN products p ON p.id = oi.product_id
            WHERE oi.order_id = $1`,
            [order.order_id]
          );
        } catch (err) {
          // Nếu lỗi, thử với schema cũ (menu_item_id, unit_price)
          itemsResult = await pool.query(
            `SELECT 
              oi.menu_item_id as product_id,
              oi.quantity,
              oi.unit_price as price,
              oi.item_name as product_name,
              p.name as product_name_from_menu
            FROM order_items oi
            LEFT JOIN menu_items p ON p.id = oi.menu_item_id
            WHERE oi.order_id = $1`,
            [order.order_id]
          );
        }
        
        // Map lại để đảm bảo format đúng và có line_total
        const mappedItems = itemsResult.rows.map(item => {
          const unitPrice = parseFloat(item.price) || 0;
          const qty = parseInt(item.quantity) || 1;
          const lineTotal = unitPrice * qty;
          
          return {
            product_id: item.product_id,
            quantity: qty,
            price: unitPrice,
            line_total: lineTotal,
            product_name: item.product_name_from_menu || item.product_name || 'Unknown'
          };
        });

        return {
          ...order,
          items: mappedItems,
          total: parseFloat(order.total_amount) || 0,
        };
      })
    );

    res.json(ordersWithItems);
  } catch (err) {
    console.error('customer recent orders error', err);
    res.status(500).json({ error: 'failed_to_fetch_orders' });
  }
});

// ---------- Cancel Order ----------
app.post('/orders/:id/cancel', requireAuth, async (req, res) => {
  const orderIdParam = req.params.id;
  const user_id = req.user.id;
  const user_role = req.user.role;
  const { reason } = req.body || {};

  try {
    // ✅ FIX: Parse orderId sang integer
    const orderId = parseInt(orderIdParam, 10);
    if (isNaN(orderId)) {
      return res.status(400).json({ error: 'invalid_order_id' });
    }

    // Kiểm tra quyền: customer chỉ hủy đơn của mình, admin/merchant có thể hủy bất kỳ
    let query = `
      SELECT id, user_id, status
      FROM orders
      WHERE id = $1
    `;
    const params = [orderId];

    if (user_role !== 'ADMIN' && user_role !== 'MERCHANT') {
      // Customer chỉ hủy đơn của mình
      query += ' AND user_id = $2';
      params.push(user_id);
    }

    const result = await pool.query(query, params);

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'order_not_found' });
    }

    const order = result.rows[0];
    
    // ✅ FIX: Normalize status để so sánh (case-insensitive)
    const orderStatus = (order.status || '').toUpperCase().trim();
    
    console.log(`[CANCEL ORDER] Order ID: ${orderId}, Current Status: "${order.status}" (normalized: "${orderStatus}")`);

    // Logic hủy đơn theo usecase:
    // - Nếu PENDING/PAID: HỦY NGAY + REFUND
    // - Nếu CONF/COOK/READY: Gửi yêu cầu hủy tới Admin/Cửa hàng (chuyển sang CANCEL_REQUESTED)
    // - Nếu PICKED_UP/DELIVERING: KH không hủy được
    
    const immediateCancelStatuses = ['PENDING']; // Hủy ngay
    const requestCancelStatuses = ['CONFIRMED', 'COOKING', 'READY']; // Yêu cầu hủy
    const nonCancellableStatuses = ['PICKED_UP', 'DELIVERING', 'DELIVERED', 'CANCELED', 'SHIPPING'];
    
    if (nonCancellableStatuses.includes(orderStatus)) {
      console.log(`[CANCEL ORDER] Cannot cancel: status "${orderStatus}" is in non-cancellable list`);
      return res.status(400).json({ 
        error: 'cannot_cancel_order', 
        message: 'Không thể hủy đơn hàng khi đã được nhận và đang giao hàng' 
      });
    }
    
    const client = await pool.connect();
    try {
      await client.query('BEGIN');

      if (immediateCancelStatuses.includes(orderStatus)) {
        // Hủy ngay
        await client.query(
          'UPDATE orders SET status = $1, updated_at = NOW() WHERE id = $2',
          ['CANCELED', orderId]
        );

        await client.query(
          'INSERT INTO order_status_history (order_id, status, note) VALUES ($1, $2, $3)',
          [orderId, 'CANCELED', reason || 'Đơn hàng bị hủy bởi khách hàng. Đã hoàn tiền (nếu có).']
        );

        await client.query('COMMIT');

        // Emit socket event
        const notificationPayload = {
          orderId,
          userId: order.user_id,
          status: 'CANCELED',
        };
        io.to(`user_${order.user_id}`).emit('orderUpdate', notificationPayload);
        io.to('admins').emit('orderCancelled', notificationPayload);

        res.json({
          success: true,
          orderId: orderId,
          status: 'CANCELED',
          message: 'Đơn hàng đã được hủy thành công'
        });
      } else if (requestCancelStatuses.includes(orderStatus)) {
        // Gửi yêu cầu hủy
        await client.query(
          'UPDATE orders SET status = $1, updated_at = NOW() WHERE id = $2',
          ['CANCEL_REQUESTED', orderId]
        );

        await client.query(
          'INSERT INTO order_status_history (order_id, status, note) VALUES ($1, $2, $3)',
          [orderId, 'CANCEL_REQUESTED', reason || 'Khách hàng yêu cầu hủy đơn. Đang chờ admin duyệt.']
        );

        await client.query('COMMIT');

        // Thông báo cho admin
        const notificationPayload = {
          orderId,
          userId: order.user_id,
          status: 'CANCEL_REQUESTED',
          reason: reason || 'Khách hàng yêu cầu hủy đơn'
        };
        io.to(`user_${order.user_id}`).emit('orderUpdate', notificationPayload);
        io.to('admins').emit('newCancelRequest', notificationPayload);

        res.json({
          success: true,
          orderId: orderId,
          status: 'CANCEL_REQUESTED',
          message: 'Yêu cầu hủy đơn đã được gửi. Đang chờ admin duyệt.'
        });
      } else {
        await client.query('ROLLBACK');
        return res.status(400).json({ 
          error: 'cannot_cancel_order', 
          message: `Không thể hủy đơn hàng ở trạng thái này (${order.status})` 
        });
      }
    } catch (err) {
      await client.query('ROLLBACK');
      throw err;
    } finally {
      client.release();
    }
  } catch (err) {
    console.error('cancel order error', err);
    res.status(500).json({ error: 'failed_to_cancel_order', message: err.message });
  }
});

// ---------- Merchant Menu Management ----------
// Lấy menu của merchant (restaurant của merchant)
app.get('/merchant/menu', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const user_role = req.user.role;

  try {
    // Kiểm tra quyền
    if (user_role !== 'MERCHANT' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    // Lấy restaurant_id của merchant
    let restaurantId;
    if (user_role === 'ADMIN') {
      // Admin có thể xem tất cả, nhưng cần restaurant_id từ query
      restaurantId = parseInt(req.query.restaurant_id, 10);
      if (isNaN(restaurantId)) {
        return res.status(400).json({ error: 'restaurant_id_required_for_admin' });
      }
    } else {
      // ✅ FIX: Tìm restaurant_id của merchant (giống logic trong POST)
      let restaurantResult = await pool.query(
        'SELECT id FROM restaurants WHERE id = $1',
        [user_id]
      );
      
      if (restaurantResult.rows.length === 0) {
        try {
          restaurantResult = await pool.query(
            'SELECT restaurant_id as id FROM user_restaurants WHERE user_id = $1',
            [user_id]
          );
        } catch (e) {
          console.log('user_restaurants table may not exist');
        }
      }
      
      if (restaurantResult.rows.length === 0) {
        // Fallback: giả sử restaurant_id = user_id
        restaurantId = user_id;
      } else {
        restaurantId = restaurantResult.rows[0].id;
      }
    }

    const result = await pool.query(
      'SELECT id, restaurant_id, name, description, price, image_url, created_at, updated_at FROM products WHERE restaurant_id = $1 ORDER BY id DESC',
      [restaurantId]
    );

    res.json(result.rows);
  } catch (err) {
    console.error('merchant menu error', err);
    res.status(500).json({ error: 'failed_to_fetch_menu', message: err.message });
  }
});

// Thêm món ăn mới
app.post('/merchant/menu', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const user_role = req.user.role;
  const { name, description, price, image_url } = req.body;

  try {
    if (user_role !== 'MERCHANT' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    if (!name || !price) {
      return res.status(400).json({ error: 'name_and_price_required' });
    }

    const priceNum = parseFloat(price);
    if (isNaN(priceNum) || priceNum < 0) {
      return res.status(400).json({ error: 'invalid_price' });
    }

    // Lấy restaurant_id của merchant
    let restaurantId;
    if (user_role === 'ADMIN') {
      restaurantId = parseInt(req.body.restaurant_id, 10);
      if (isNaN(restaurantId)) {
        return res.status(400).json({ error: 'restaurant_id_required_for_admin' });
      }
    } else {
      // ✅ FIX: Tìm restaurant_id của merchant
      // Thử tìm restaurant có id = user_id trước (nếu merchant user_id = restaurant_id)
      let restaurantResult = await pool.query(
        'SELECT id FROM restaurants WHERE id = $1',
        [user_id]
      );
      
      // Nếu không tìm thấy, thử tìm trong bảng user_restaurants (nếu có)
      if (restaurantResult.rows.length === 0) {
        try {
          restaurantResult = await pool.query(
            'SELECT restaurant_id as id FROM user_restaurants WHERE user_id = $1',
            [user_id]
          );
        } catch (e) {
          // Bảng user_restaurants có thể không tồn tại, bỏ qua
          console.log('user_restaurants table may not exist, using fallback');
        }
      }
      
      // Nếu vẫn không tìm thấy, tạo restaurant mới cho merchant
      if (restaurantResult.rows.length === 0) {
        console.log(`[MERCHANT MENU] No restaurant found for user ${user_id}, creating new restaurant`);
        // ✅ FIX: Tạo restaurant mới (không set id, để database tự generate)
        const userResult = await pool.query('SELECT username FROM users WHERE id = $1', [user_id]);
        const username = userResult.rows[0]?.username || `Merchant ${user_id}`;
        
        try {
          // Tạo restaurant mới, không set id (để SERIAL tự động tăng)
          const newRestaurantResult = await pool.query(
            'INSERT INTO restaurants (name, address, rating) VALUES ($1, $2, $3) RETURNING id',
            [`${username}'s Restaurant`, 'Chưa cập nhật', 0]
          );
          restaurantId = newRestaurantResult.rows[0].id;
          console.log(`[MERCHANT MENU] Created restaurant ${restaurantId} for user ${user_id}`);
          
          // ✅ FIX: Lưu mapping vào bảng user_restaurants nếu có, hoặc tạo bảng nếu chưa có
          try {
            // Thử tạo bảng user_restaurants nếu chưa có
            await pool.query(`
              CREATE TABLE IF NOT EXISTS user_restaurants (
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                restaurant_id INTEGER NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
                PRIMARY KEY (user_id, restaurant_id)
              )
            `);
            // Lưu mapping
            await pool.query(
              'INSERT INTO user_restaurants (user_id, restaurant_id) VALUES ($1, $2) ON CONFLICT (user_id, restaurant_id) DO NOTHING',
              [user_id, restaurantId]
            );
          } catch (e) {
            // Nếu không tạo được bảng hoặc insert được, bỏ qua (không critical)
            console.log(`[MERCHANT MENU] Could not create/save user_restaurants mapping: ${e.message}`);
          }
        } catch (createErr) {
          console.error(`[MERCHANT MENU] Error creating restaurant:`, createErr);
          return res.status(500).json({ error: 'failed_to_create_restaurant', message: createErr.message });
        }
      } else {
        restaurantId = restaurantResult.rows[0].id;
      }
    }

    console.log(`[MERCHANT MENU] Adding menu item for restaurant_id: ${restaurantId}, user_id: ${user_id}`);

    // ✅ FIX: Đảm bảo bảng products tồn tại và có đầy đủ cột
    try {
      // Tạo bảng nếu chưa có
      await pool.query(`
        CREATE TABLE IF NOT EXISTS products (
          id SERIAL PRIMARY KEY,
          restaurant_id INTEGER NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
          name VARCHAR(255) NOT NULL,
          description TEXT,
          price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
          image_url TEXT
        )
      `);
      
      // Thêm các cột created_at và updated_at nếu chưa có
      try {
        await pool.query('ALTER TABLE products ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW()');
        await pool.query('ALTER TABLE products ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW()');
        console.log('[MERCHANT MENU] Products table columns ensured');
      } catch (colErr) {
        console.log('[MERCHANT MENU] Error adding columns (may already exist):', colErr.message);
      }
      
      console.log('[MERCHANT MENU] Products table ensured');
    } catch (tableErr) {
      console.log('[MERCHANT MENU] Products table may already exist or error:', tableErr.message);
    }

    // ✅ FIX: Kiểm tra xem bảng có cột created_at và updated_at không
    let returnColumns = 'id, restaurant_id, name, description, price, image_url';
    try {
      const colCheck = await pool.query(`
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'products' 
        AND column_name IN ('created_at', 'updated_at')
      `);
      const hasCreatedAt = colCheck.rows.some(r => r.column_name === 'created_at');
      const hasUpdatedAt = colCheck.rows.some(r => r.column_name === 'updated_at');
      
      if (hasCreatedAt) returnColumns += ', created_at';
      if (hasUpdatedAt) returnColumns += ', updated_at';
    } catch (checkErr) {
      console.log('[MERCHANT MENU] Could not check columns, using basic return:', checkErr.message);
    }

    const result = await pool.query(
      `INSERT INTO products (restaurant_id, name, description, price, image_url) VALUES ($1, $2, $3, $4, $5) RETURNING ${returnColumns}`,
      [restaurantId, name, description || null, priceNum, image_url || null]
    );

    res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error('merchant add menu error', err);
    res.status(500).json({ error: 'failed_to_add_menu_item', message: err.message });
  }
});

// Cập nhật món ăn
app.patch('/merchant/menu/:id', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const user_role = req.user.role;
  const menuId = parseInt(req.params.id, 10);
  const { name, description, price, image_url } = req.body;

  try {
    if (isNaN(menuId)) {
      return res.status(400).json({ error: 'invalid_menu_id' });
    }

    if (user_role !== 'MERCHANT' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    console.log(`[MERCHANT UPDATE] Attempting to update menu item ${menuId} for user ${user_id}`);

    // ✅ FIX: Kiểm tra quyền - tìm restaurant_id của merchant
    let merchantRestaurantId = null;
    if (user_role !== 'ADMIN') {
      // Tìm restaurant_id của merchant (giống logic trong POST)
      let restaurantResult = await pool.query(
        'SELECT id FROM restaurants WHERE id = $1',
        [user_id]
      );
      
      if (restaurantResult.rows.length === 0) {
        try {
          restaurantResult = await pool.query(
            'SELECT restaurant_id as id FROM user_restaurants WHERE user_id = $1',
            [user_id]
          );
        } catch (e) {
          console.log('[MERCHANT UPDATE] user_restaurants table may not exist');
        }
      }
      
      if (restaurantResult.rows.length === 0) {
        merchantRestaurantId = user_id; // Fallback
      } else {
        merchantRestaurantId = restaurantResult.rows[0].id;
      }
      console.log(`[MERCHANT UPDATE] Merchant restaurant_id: ${merchantRestaurantId}`);
    }

    // Kiểm tra món ăn có tồn tại và thuộc về restaurant của merchant không
    let query = 'SELECT id, restaurant_id, name FROM products WHERE id = $1';
    const params = [menuId];

    if (user_role !== 'ADMIN' && merchantRestaurantId !== null) {
      query += ' AND restaurant_id = $2';
      params.push(merchantRestaurantId);
    }

    const checkResult = await pool.query(query, params);
    if (checkResult.rows.length === 0) {
      console.log(`[MERCHANT UPDATE] Menu item ${menuId} not found or not owned by merchant`);
      return res.status(404).json({ error: 'menu_item_not_found' });
    }

    // Build update query
    const updates = [];
    const updateParams = [];
    let paramIndex = 1;

    if (name !== undefined) {
      updates.push(`name = $${paramIndex++}`);
      updateParams.push(name);
    }
    if (description !== undefined) {
      updates.push(`description = $${paramIndex++}`);
      updateParams.push(description);
    }
    if (price !== undefined) {
      const priceNum = parseFloat(price);
      if (isNaN(priceNum) || priceNum < 0) {
        return res.status(400).json({ error: 'invalid_price' });
      }
      updates.push(`price = $${paramIndex++}`);
      updateParams.push(priceNum);
    }
    if (image_url !== undefined) {
      updates.push(`image_url = $${paramIndex++}`);
      updateParams.push(image_url);
    }

    if (updates.length === 0) {
      return res.status(400).json({ error: 'no_fields_to_update' });
    }

    // ✅ FIX: Kiểm tra xem cột updated_at có tồn tại không
    let hasUpdatedAt = false;
    try {
      const colCheck = await pool.query(`
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'products' 
        AND column_name = 'updated_at'
      `);
      hasUpdatedAt = colCheck.rows.length > 0;
    } catch (checkErr) {
      console.log('[MERCHANT UPDATE] Could not check updated_at column');
    }

    if (hasUpdatedAt) {
      updates.push(`updated_at = NOW()`);
    }
    updateParams.push(menuId);

    // ✅ FIX: Kiểm tra các cột trước khi RETURNING
    let returnColumns = 'id, restaurant_id, name, description, price, image_url';
    try {
      const colCheck = await pool.query(`
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'products' 
        AND column_name IN ('created_at', 'updated_at')
      `);
      const hasCreatedAt = colCheck.rows.some(r => r.column_name === 'created_at');
      const hasUpdatedAtCol = colCheck.rows.some(r => r.column_name === 'updated_at');
      
      if (hasCreatedAt) returnColumns += ', created_at';
      if (hasUpdatedAtCol) returnColumns += ', updated_at';
    } catch (checkErr) {
      console.log('[MERCHANT UPDATE] Could not check columns, using basic return');
    }

    const updateQuery = `
      UPDATE products 
      SET ${updates.join(', ')}
      WHERE id = $${paramIndex}
      RETURNING ${returnColumns}
    `;

    console.log(`[MERCHANT UPDATE] Update query: ${updateQuery}`);
    console.log(`[MERCHANT UPDATE] Update params:`, updateParams);

    const result = await pool.query(updateQuery, updateParams);
    
    if (result.rows.length === 0) {
      console.log(`[MERCHANT UPDATE] No rows updated for menu item ${menuId}`);
      return res.status(404).json({ error: 'menu_item_not_found' });
    }

    console.log(`[MERCHANT UPDATE] Successfully updated menu item ${menuId}`);
    res.json(result.rows[0]);
  } catch (err) {
    console.error('merchant update menu error', err);
    res.status(500).json({ error: 'failed_to_update_menu_item', message: err.message });
  }
});

// Xóa món ăn
app.delete('/merchant/menu/:id', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const user_role = req.user.role;
  const menuId = parseInt(req.params.id, 10);

  try {
    if (isNaN(menuId)) {
      return res.status(400).json({ error: 'invalid_menu_id' });
    }

    if (user_role !== 'MERCHANT' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    console.log(`[MERCHANT DELETE] Attempting to delete menu item ${menuId} for user ${user_id}`);

    // ✅ FIX: Kiểm tra quyền - tìm restaurant_id của merchant
    let merchantRestaurantId = null;
    if (user_role !== 'ADMIN') {
      // Tìm restaurant_id của merchant (giống logic trong POST)
      let restaurantResult = await pool.query(
        'SELECT id FROM restaurants WHERE id = $1',
        [user_id]
      );
      
      if (restaurantResult.rows.length === 0) {
        try {
          restaurantResult = await pool.query(
            'SELECT restaurant_id as id FROM user_restaurants WHERE user_id = $1',
            [user_id]
          );
        } catch (e) {
          console.log('[MERCHANT DELETE] user_restaurants table may not exist');
        }
      }
      
      if (restaurantResult.rows.length === 0) {
        merchantRestaurantId = user_id; // Fallback
      } else {
        merchantRestaurantId = restaurantResult.rows[0].id;
      }
      console.log(`[MERCHANT DELETE] Merchant restaurant_id: ${merchantRestaurantId}`);
    }

    // Kiểm tra món ăn có tồn tại và thuộc về restaurant của merchant không
    let query = 'SELECT id, restaurant_id, name FROM products WHERE id = $1';
    const params = [menuId];

    if (user_role !== 'ADMIN' && merchantRestaurantId !== null) {
      query += ' AND restaurant_id = $2';
      params.push(merchantRestaurantId);
    }

    const checkResult = await pool.query(query, params);
    if (checkResult.rows.length === 0) {
      console.log(`[MERCHANT DELETE] Menu item ${menuId} not found or not owned by merchant`);
      return res.status(404).json({ error: 'menu_item_not_found' });
    }

    const menuItem = checkResult.rows[0];
    console.log(`[MERCHANT DELETE] Deleting menu item: ${menuItem.name} (id: ${menuId})`);

    // Xóa món ăn
    const deleteResult = await pool.query('DELETE FROM products WHERE id = $1', [menuId]);
    
    if (deleteResult.rowCount === 0) {
      console.log(`[MERCHANT DELETE] No rows deleted for menu item ${menuId}`);
      return res.status(404).json({ error: 'menu_item_not_found' });
    }

    console.log(`[MERCHANT DELETE] Successfully deleted menu item ${menuId}`);
    res.json({ success: true, message: 'Menu item deleted', id: menuId });
  } catch (err) {
    console.error('merchant delete menu error', err);
    res.status(500).json({ error: 'failed_to_delete_menu_item', message: err.message });
  }
});

// ---------- Upload Image ----------
// Upload ảnh sản phẩm
app.post('/upload/image', requireAuth, upload.single('image'), (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'no_file_uploaded', message: 'Không có file ảnh được upload' });
    }

    const user_role = req.user.role;
    
    // Chỉ cho phép MERCHANT và ADMIN upload ảnh
    if (user_role !== 'MERCHANT' && user_role !== 'ADMIN') {
      // Xóa file nếu không có quyền
      if (req.file.path) {
        fs.unlinkSync(req.file.path);
      }
      return res.status(403).json({ error: 'forbidden', message: 'Không có quyền upload ảnh' });
    }

    // Tạo URL để truy cập ảnh
    // Lấy base URL từ request (hoặc dùng env)
    const protocol = req.protocol;
    const host = req.get('host');
    const imageUrl = `${protocol}://${host}/uploads/${req.file.filename}`;

    console.log(`[UPLOAD IMAGE] Successfully uploaded: ${req.file.filename} by user ${req.user.id}`);

    res.json({
      success: true,
      url: imageUrl,
      filename: req.file.filename,
      size: req.file.size
    });
  } catch (err) {
    console.error('upload image error', err);
    
    // Xóa file nếu có lỗi
    if (req.file && req.file.path) {
      try {
        fs.unlinkSync(req.file.path);
      } catch (unlinkErr) {
        console.error('Error deleting uploaded file:', unlinkErr);
      }
    }

    res.status(500).json({ 
      error: 'upload_failed', 
      message: err.message || 'Không thể upload ảnh' 
    });
  }
});

// ---------- Merchant Orders ----------
// Lấy đơn hàng của merchant
app.get('/merchant/orders', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const user_role = req.user.role;
  const { status } = req.query;

  try {
    if (user_role !== 'MERCHANT' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    // Lấy restaurant_id của merchant
    let restaurantId;
    if (user_role === 'ADMIN') {
      restaurantId = parseInt(req.query.restaurant_id, 10);
      if (isNaN(restaurantId)) {
        return res.status(400).json({ error: 'restaurant_id_required_for_admin' });
      }
    } else {
      const restaurantResult = await pool.query(
        'SELECT id FROM restaurants WHERE id = $1 OR id IN (SELECT restaurant_id FROM user_restaurants WHERE user_id = $1)',
        [user_id]
      );
      if (restaurantResult.rows.length === 0) {
        restaurantId = user_id;
      } else {
        restaurantId = restaurantResult.rows[0].id;
      }
    }

    // Map frontend status to database status
    // ✅ FIX: Map đúng status từ frontend sang database, và hỗ trợ multiple status cho mỗi bucket
    let query = `
      SELECT 
        o.id as order_id,
        o.status,
        COALESCE(o.total_amount, o.total, 0) as total,
        COALESCE(o.total_amount, o.total, 0) as total_amount,
        o.created_at,
        o.address,
        o.payment_method,
        u.username as customer_name
      FROM orders o
      LEFT JOIN users u ON u.id = o.user_id
      WHERE o.restaurant_id = $1
    `;
    const params = [restaurantId];

    if (status) {
      const statusLower = status.toLowerCase();
      // ✅ FIX: Map frontend bucket sang database status(es)
      // "pending" bucket: PENDING và CONFIRMED (cả hai đều là "chờ xác nhận")
      // "preparing" bucket: COOKING và PREPARING (đang chuẩn bị)
      // "ready" bucket: READY và HANDOVER (sẵn sàng)
      if (statusLower === 'pending' || statusLower === 'new') {
        query += ' AND (o.status = $2 OR o.status = $3)';
        params.push('PENDING', 'CONFIRMED');
        console.log(`[MERCHANT ORDERS] Querying orders with status: ${status} -> PENDING or CONFIRMED for restaurant_id=${restaurantId}`);
      } else if (statusLower === 'preparing' || statusLower === 'in_progress') {
        query += ' AND (o.status = $2 OR o.status = $3)';
        params.push('COOKING', 'PREPARING');
        console.log(`[MERCHANT ORDERS] Querying orders with status: ${status} -> COOKING or PREPARING for restaurant_id=${restaurantId}`);
      } else if (statusLower === 'ready') {
        query += ' AND (o.status = $2 OR o.status = $3)';
        params.push('READY', 'HANDOVER');
        console.log(`[MERCHANT ORDERS] Querying orders with status: ${status} -> READY or HANDOVER for restaurant_id=${restaurantId}`);
      } else if (statusLower === 'completed' || statusLower === 'delivered') {
        // ✅ FIX: Tab "completed" chỉ hiển thị đơn DELIVERED (đã giao thành công)
        // READY (sẵn sàng) chỉ là đã làm xong, chờ shipper, chưa phải "hoàn tất"
        query += ' AND o.status = $2';
        params.push('DELIVERED');
        console.log(`[MERCHANT ORDERS] Querying orders with status: ${status} -> DELIVERED for restaurant_id=${restaurantId}`);
      } else if (statusLower === 'cancelled' || statusLower === 'canceled') {
        query += ' AND o.status = $2';
        params.push('CANCELED');
        console.log(`[MERCHANT ORDERS] Querying orders with status: ${status} -> CANCELED for restaurant_id=${restaurantId}`);
      } else {
        // Fallback: dùng status trực tiếp
        const dbStatus = status.toUpperCase();
      query += ' AND o.status = $2';
      params.push(dbStatus);
        console.log(`[MERCHANT ORDERS] Querying orders with status: ${status} -> ${dbStatus} (fallback) for restaurant_id=${restaurantId}`);
      }
    } else {
      console.log(`[MERCHANT ORDERS] Querying all orders for restaurant_id=${restaurantId}`);
    }

    query += ' ORDER BY o.created_at DESC';

    const result = await pool.query(query, params);
    console.log(`[MERCHANT ORDERS] Found ${result.rows.length} order(s)`);
    
    // ✅ FIX: Tính lại total từ items nếu total = 0
    const ordersWithItems = await Promise.all(
      result.rows.map(async (order) => {
        let itemsResult;
        try {
          // Thử với schema mới (product_id, price)
          itemsResult = await pool.query(
            `SELECT 
              oi.product_id,
              oi.quantity,
              oi.price,
              p.name as product_name
            FROM order_items oi
            LEFT JOIN products p ON p.id = oi.product_id
            WHERE oi.order_id = $1`,
            [order.order_id]
          );
        } catch (err) {
          // Nếu lỗi, thử với schema cũ (menu_item_id, unit_price)
          itemsResult = await pool.query(
            `SELECT 
              oi.menu_item_id as product_id,
              oi.quantity,
              oi.unit_price as price,
              oi.item_name as product_name,
              p.name as product_name_from_menu
            FROM order_items oi
            LEFT JOIN menu_items p ON p.id = oi.menu_item_id
            WHERE oi.order_id = $1`,
            [order.order_id]
          );
        }
        
        // Map lại để đảm bảo format đúng
        const mappedItems = itemsResult.rows.map(item => {
          const unitPrice = parseFloat(item.price) || 0;
          const qty = parseInt(item.quantity) || 1;
          const lineTotal = unitPrice * qty;
          
          return {
            product_id: item.product_id,
            quantity: qty,
            price: unitPrice,
            line_total: lineTotal,
            product_name: item.product_name_from_menu || item.product_name || 'Unknown'
          };
        });
        
        // ✅ FIX: Luôn tính lại total từ items để đảm bảo chính xác (dùng line_total đã tính sẵn)
        let finalTotal = 0;
        if (mappedItems.length > 0) {
          finalTotal = mappedItems.reduce((sum, item) => {
            // Ưu tiên dùng line_total (đã tính sẵn), nếu không có thì tính lại
            const lineTotal = parseFloat(item.line_total) || (parseFloat(item.price) * parseInt(item.quantity) || 0);
            return sum + lineTotal;
          }, 0);
          const dbTotal = parseFloat(order.total) || parseFloat(order.total_amount) || 0;
          if (Math.abs(finalTotal - dbTotal) > 0.01) {
            console.log(`[MERCHANT ORDERS] Calculated total from items for order ${order.order_id}: ${finalTotal} (DB had: ${dbTotal})`);
          }
        } else {
          finalTotal = parseFloat(order.total) || parseFloat(order.total_amount) || 0;
        }
        
        return {
          ...order,
          total: finalTotal,
          total_amount: finalTotal,
          items: mappedItems
        };
      })
    );
    
    res.json(ordersWithItems);
  } catch (err) {
    console.error('merchant orders error', err);
    res.status(500).json({ error: 'failed_to_fetch_orders' });
  }
});

// Cập nhật trạng thái đơn hàng (merchant)
app.patch('/merchant/orders/:id/status', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const user_role = req.user.role;
  const orderId = parseInt(req.params.id, 10);
  const { status } = req.body;

  try {
    if (isNaN(orderId)) {
      return res.status(400).json({ error: 'invalid_order_id' });
    }

    if (user_role !== 'MERCHANT' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    if (!status) {
      return res.status(400).json({ error: 'status_required' });
    }

    // Kiểm tra quyền và lấy order
    console.log(`[MERCHANT UPDATE STATUS] User ${user_id} (${user_role}) attempting to update order ${orderId}`);
    
    let order;
    
    if (user_role !== 'ADMIN') {
      // Tìm tất cả restaurant_id của merchant
      const restaurantResult = await pool.query(
        'SELECT restaurant_id as id FROM user_restaurants WHERE user_id = $1',
        [user_id]
      );
      
      let merchantRestaurantIds = [];
      if (restaurantResult.rows.length > 0) {
        merchantRestaurantIds = restaurantResult.rows.map(r => r.id);
        console.log(`[MERCHANT UPDATE STATUS] Merchant has restaurants: ${merchantRestaurantIds.join(', ')}`);
      } else {
        // Fallback: thử tìm restaurant với id = user_id
        const fallbackResult = await pool.query(
          'SELECT id FROM restaurants WHERE id = $1',
          [user_id]
        );
        if (fallbackResult.rows.length > 0) {
          merchantRestaurantIds = [user_id];
          console.log(`[MERCHANT UPDATE STATUS] Using fallback restaurant_id: ${user_id}`);
        } else {
          merchantRestaurantIds = [user_id]; // Last resort
          console.log(`[MERCHANT UPDATE STATUS] No restaurant found, using user_id as fallback: ${user_id}`);
        }
      }

      // Kiểm tra order có thuộc về một trong các restaurant của merchant không
      const checkResult = await pool.query(
        'SELECT restaurant_id, status FROM orders WHERE id = $1 AND restaurant_id = ANY($2::int[])',
        [orderId, merchantRestaurantIds]
      );
      
      if (checkResult.rows.length === 0) {
        console.log(`[MERCHANT UPDATE STATUS] ❌ Order ${orderId} not found or not owned by merchant's restaurants`);
        return res.status(404).json({ error: 'order_not_found' });
      }
      
      order = checkResult.rows[0];
    } else {
      // ADMIN: không cần kiểm tra restaurant
      const checkResult = await pool.query(
        'SELECT restaurant_id, status FROM orders WHERE id = $1',
        [orderId]
      );
      
      if (checkResult.rows.length === 0) {
        console.log(`[MERCHANT UPDATE STATUS] ❌ Order ${orderId} not found`);
        return res.status(404).json({ error: 'order_not_found' });
      }
      
      order = checkResult.rows[0];
    }

    // Map frontend status to database status
    // Database schema: PENDING, CONFIRMED, COOKING, READY, PICKED_UP, DELIVERING, DELIVERED, CANCEL_REQUESTED, CANCELED
    const statusMap = {
      'preparing': 'COOKING', // preparing -> COOKING
      'in_progress': 'COOKING', // in_progress -> COOKING
      'new': 'PENDING', // new -> PENDING
      'pending': 'PENDING',
      'confirmed': 'CONFIRMED',
      'ready': 'READY',
      'shipping': 'DELIVERING', // shipping -> DELIVERING
      'delivering': 'DELIVERING',
      'delivered': 'DELIVERED',
      'completed': 'DELIVERED',
      'cancelled': 'CANCELED',
      'canceled': 'CANCELED',
      'cancel_requested': 'CANCEL_REQUESTED'
    };
    
    const statusLower = (status || '').toLowerCase().trim();
    const newStatus = statusMap[statusLower];
    
    if (!newStatus) {
      console.error(`[MERCHANT UPDATE STATUS] ❌ Invalid status: "${status}" (normalized: "${statusLower}")`);
      return res.status(400).json({ error: 'invalid_status', received: status, allowed: Object.keys(statusMap) });
    }

    console.log(`[MERCHANT UPDATE STATUS] Status mapping: "${status}" -> "${newStatus}"`);
    console.log(`[MERCHANT UPDATE STATUS] Updating order ${orderId} from ${order.status} to ${newStatus} by user ${user_id}`);
    
    // Lấy user_id của order để emit socket
    const orderInfo = await pool.query('SELECT user_id FROM orders WHERE id = $1', [orderId]);
    const orderUserId = orderInfo.rows[0]?.user_id;
    
    // Cập nhật trạng thái
    await pool.query(
      'UPDATE orders SET status = $1, updated_at = NOW() WHERE id = $2',
      [newStatus, orderId]
    );

    // Thêm vào lịch sử (nếu bảng tồn tại)
    try {
      await pool.query(
        'INSERT INTO order_status_history (order_id, status, note) VALUES ($1, $2, $3)',
        [orderId, newStatus, `Status updated by merchant`]
      );
    } catch (historyErr) {
      // Nếu bảng không tồn tại, chỉ log warning, không fail request
      console.warn('[MERCHANT UPDATE STATUS] Could not insert into order_status_history:', historyErr.message);
    }

    // ✅ FIX: Emit socket để customer nhận được update real-time
    if (orderUserId) {
      io.to(`order_${orderId}`).emit('statusUpdate', {
        orderId,
        status: newStatus,
      });
      io.to(`user_${orderUserId}`).emit('orderUpdate', {
        orderId,
        status: newStatus,
      });
      console.log(`[MERCHANT UPDATE STATUS] ✅ Emitted socket update to user ${orderUserId} for order ${orderId}`);
    }

    console.log(`[MERCHANT UPDATE STATUS] ✅ Successfully updated order ${orderId} to ${newStatus}`);
    res.json({ success: true, orderId, status: newStatus });
  } catch (err) {
    console.error('[MERCHANT UPDATE STATUS] ❌ Error:', err);
    console.error('[MERCHANT UPDATE STATUS] Error details:', {
      message: err.message,
      stack: err.stack
    });
    res.status(500).json({ error: 'failed_to_update_order_status', message: err.message });
  }
});

// ---------- Shipper Orders ----------
// Lấy danh sách đơn hàng cho shipper
app.get('/shipper/orders', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const user_role = req.user.role;
  const { status } = req.query;

  try {
    if (user_role !== 'SHIPPER' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    // Kiểm tra shipper có trong bảng shippers không
    const shipperCheck = await pool.query('SELECT id FROM shippers WHERE id = $1', [user_id]);
    if (shipperCheck.rows.length === 0 && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'not_a_shipper' });
    }

    let query = `
      SELECT 
        o.id as order_id,
        o.status,
        o.shipper_id,
        COALESCE(o.total_amount, o.total, 0) as total,
        COALESCE(o.total_amount, o.total, 0) as total_amount,
        o.created_at,
        o.address,
        o.payment_method,
        o.restaurant_id,
        r.name as restaurant_name,
        u.username as customer_name
      FROM orders o
      LEFT JOIN restaurants r ON r.id = o.restaurant_id
      LEFT JOIN users u ON u.id = o.user_id
      WHERE 1=1
    `;
    const params = [];

    // Filter theo status
    if (status) {
      const statusLower = status.toLowerCase();
      if (statusLower === 'available') {
        // Đơn hàng sẵn sàng để nhận (PENDING, CONFIRMED, COOKING, READY - chưa có shipper)
        query += ' AND o.status IN ($1, $2, $3, $4) AND o.shipper_id IS NULL';
        params.push('PENDING', 'CONFIRMED', 'COOKING', 'READY');
      } else if (statusLower === 'delivering') {
        // ✅ FIX: Đơn hàng đang giao - tìm orders có shipper_id = user_id và status = SHIPPING
        // Hoặc nếu status không hợp lệ với constraint nhưng đã có shipper_id (đã accept)
        // ✅ FIX: Đơn hàng đang giao - tìm orders có shipper_id = user_id và status != DELIVERED, CANCELED
        // (bao gồm cả SHIPPING và các status khác như READY, CONFIRMED, COOKING nếu đã được assign)
        query += ' AND o.shipper_id = $1 AND o.status != $2 AND o.status != $3';
        params.push(user_id, 'DELIVERED', 'CANCELED');
        console.log(`[SHIPPER ORDERS] Query delivering orders for shipper ${user_id} (excluding DELIVERED and CANCELED)`);
      } else if (statusLower === 'completed') {
        // Đơn hàng đã hoàn tất (DELIVERED, shipper_id = user_id)
        query += ' AND o.shipper_id = $1 AND o.status = $2';
        params.push(user_id, 'DELIVERED');
      } else {
        // Fallback: dùng status trực tiếp
        query += ' AND o.status = $1';
        params.push(status.toUpperCase());
      }
    } else {
      // ✅ FIX: Nếu không có status (tab "MỚI"), hiển thị tất cả đơn chưa có shipper và có thể nhận
      // Bao gồm: PENDING, CONFIRMED, COOKING, READY (chưa có shipper)
      query += ' AND o.shipper_id IS NULL AND o.status IN ($1, $2, $3, $4)';
      params.push('PENDING', 'CONFIRMED', 'COOKING', 'READY');
      console.log(`[SHIPPER ORDERS] Querying available orders (no status param) for shipper ${user_id}`);
    }

    query += ' ORDER BY o.created_at DESC';

    console.log(`[SHIPPER ORDERS] Executing query for status=${status}, user_id=${user_id}`);
    console.log(`[SHIPPER ORDERS] Query: ${query}`);
    console.log(`[SHIPPER ORDERS] Params:`, params);
    
    const result = await pool.query(query, params);
    console.log(`[SHIPPER ORDERS] Found ${result.rows.length} order(s) for shipper ${user_id} with status=${status || 'all'}`);
    if (result.rows.length > 0) {
      console.log(`[SHIPPER ORDERS] Sample order:`, JSON.stringify(result.rows[0], null, 2));
    }
    
    // ✅ FIX: Tính lại total từ items để đảm bảo chính xác (giống như merchant orders)
    const ordersWithItems = await Promise.all(
      result.rows.map(async (order) => {
        let itemsResult;
        try {
          // Thử với schema mới (product_id, price)
          itemsResult = await pool.query(
            `SELECT 
              oi.product_id,
              oi.quantity,
              oi.price,
              p.name as product_name
            FROM order_items oi
            LEFT JOIN products p ON p.id = oi.product_id
            WHERE oi.order_id = $1`,
            [order.order_id]
          );
        } catch (err) {
          // Nếu lỗi, thử với schema cũ (menu_item_id, unit_price)
          itemsResult = await pool.query(
            `SELECT 
              oi.menu_item_id as product_id,
              oi.quantity,
              oi.unit_price as price,
              oi.item_name as product_name,
              p.name as product_name_from_menu
            FROM order_items oi
            LEFT JOIN menu_items p ON p.id = oi.menu_item_id
            WHERE oi.order_id = $1`,
            [order.order_id]
          );
        }
        
        // Map lại để đảm bảo format đúng
        const mappedItems = itemsResult.rows.map(item => {
          const unitPrice = parseFloat(item.price) || 0;
          const qty = parseInt(item.quantity) || 1;
          const lineTotal = unitPrice * qty;
          
          return {
            product_id: item.product_id,
            quantity: qty,
            price: unitPrice,
            line_total: lineTotal,
            product_name: item.product_name_from_menu || item.product_name || 'Unknown'
          };
        });
        
        // ✅ FIX: Luôn tính lại total từ items để đảm bảo chính xác (dùng line_total đã tính sẵn)
        let finalTotal = 0;
        if (mappedItems.length > 0) {
          finalTotal = mappedItems.reduce((sum, item) => {
            // Ưu tiên dùng line_total (đã tính sẵn), nếu không có thì tính lại
            const lineTotal = parseFloat(item.line_total) || (parseFloat(item.price) * parseInt(item.quantity) || 0);
            return sum + lineTotal;
          }, 0);
          const dbTotal = parseFloat(order.total) || parseFloat(order.total_amount) || 0;
          if (Math.abs(finalTotal - dbTotal) > 0.01) {
            console.log(`[SHIPPER ORDERS] Calculated total from items for order ${order.order_id}: ${finalTotal} (DB had: ${dbTotal})`);
          }
        } else {
          finalTotal = parseFloat(order.total) || parseFloat(order.total_amount) || 0;
        }
        
        return {
          ...order,
          total: finalTotal,
          total_amount: finalTotal,
          items: mappedItems
        };
      })
    );
    
    res.json(ordersWithItems);
  } catch (err) {
    console.error('shipper orders error', err);
    res.status(500).json({ error: 'failed_to_fetch_orders' });
  }
});

// Nhận đơn hàng (shipper accept order)
app.post('/shipper/orders/:id/accept', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const user_role = req.user.role;
  const orderId = parseInt(req.params.id, 10);

  try {
    if (user_role !== 'SHIPPER' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    if (isNaN(orderId)) {
      return res.status(400).json({ error: 'invalid_order_id' });
    }

    // Kiểm tra đơn hàng có sẵn sàng để nhận không (READY, chưa có shipper)
    const orderResult = await pool.query(
      'SELECT id, status, shipper_id FROM orders WHERE id = $1',
      [orderId]
    );

    if (orderResult.rows.length === 0) {
      return res.status(404).json({ error: 'order_not_found' });
    }

    const order = orderResult.rows[0];
    // ✅ FIX: Cho phép accept nếu status là READY hoặc đơn chưa có shipper (available)
    if (order.shipper_id != null) {
      return res.status(400).json({ error: 'order_already_assigned' });
    }
    
    // ✅ FIX: Cho phép accept nếu status là READY, CONFIRMED, COOKING, hoặc bất kỳ status nào chưa có shipper
    const allowedStatuses = ['READY', 'CONFIRMED', 'COOKING', 'PENDING'];
    if (!allowedStatuses.includes(order.status)) {
      console.log(`[SHIPPER ACCEPT] Order ${orderId} has status ${order.status}, not in allowed list`);
      // Vẫn cho phép accept nếu đơn chưa có shipper
      if (order.shipper_id != null) {
        return res.status(400).json({ error: 'order_not_ready', current_status: order.status });
      }
    }

    // Kiểm tra shipper có available không
    const shipperCheck = await pool.query(
      'SELECT available FROM shippers WHERE id = $1',
      [user_id]
    );

    if (shipperCheck.rows.length === 0) {
      return res.status(403).json({ error: 'not_a_shipper' });
    }

    // ✅ FIX: Không kiểm tra available nữa (cho phép nhận nhiều đơn)
    // if (!shipperCheck.rows[0].available) {
    //   return res.status(400).json({ error: 'shipper_not_available' });
    // }

    // ✅ FIX: Gán đơn hàng cho shipper
    // Database constraint chỉ cho phép: PENDING, PREPARING, SHIPPING, DELIVERED, CANCELED
    // Constraint có thể không cho phép chuyển trực tiếp từ PENDING sang SHIPPING
    // Nên chỉ update shipper_id, giữ nguyên status hiện tại để tránh constraint violation
    const currentStatus = order.status;
    let newStatus = currentStatus; // Giữ nguyên status hiện tại
    
    try {
      // Thử update cả shipper_id và status = SHIPPING
      await pool.query(
        'UPDATE orders SET shipper_id = $1, status = $2, updated_at = NOW() WHERE id = $3',
        [user_id, 'SHIPPING', orderId]
      );
      newStatus = 'SHIPPING';
      console.log(`[SHIPPER ACCEPT] ✅ Updated order ${orderId} to SHIPPING`);
    } catch (updateErr) {
      // Nếu bị constraint violation, chỉ update shipper_id
      if (updateErr.code === '23514' && updateErr.constraint === 'orders_status_check') {
        console.log(`[SHIPPER ACCEPT] ⚠️ Constraint violation when setting SHIPPING, only updating shipper_id for order ${orderId}`);
        await pool.query(
          'UPDATE orders SET shipper_id = $1, updated_at = NOW() WHERE id = $2',
          [user_id, orderId]
        );
        newStatus = currentStatus; // Giữ nguyên status
      } else {
        // Lỗi khác, throw lại
        throw updateErr;
      }
    }

    // Thêm vào lịch sử
    try {
      await pool.query(
        'INSERT INTO order_status_history (order_id, status, note) VALUES ($1, $2, $3)',
        [orderId, newStatus, `Shipper ${user_id} accepted and started delivery`]
      );
    } catch (historyErr) {
      console.warn('[SHIPPER ACCEPT] Could not insert into order_status_history:', historyErr.message);
    }

    // ✅ FIX: KHÔNG set shipper unavailable ngay (cho phép nhận nhiều đơn)
    // await pool.query('UPDATE shippers SET available = false WHERE id = $1', [user_id]);

    // Emit socket events
    const orderInfo = await pool.query('SELECT user_id, restaurant_id FROM orders WHERE id = $1', [orderId]);
    const orderUserId = orderInfo.rows[0]?.user_id;
    const orderRestaurantId = orderInfo.rows[0]?.restaurant_id;
    
    // Lấy lại order sau khi update để có status mới nhất
    const updatedOrderResult = await pool.query('SELECT status FROM orders WHERE id = $1', [orderId]);
    const finalStatus = updatedOrderResult.rows[0]?.status || newStatus;
    
    if (orderUserId) {
      io.to(`order_${orderId}`).emit('statusUpdate', {
        orderId,
        status: finalStatus,
        shipperId: user_id,
      });
      io.to(`user_${orderUserId}`).emit('orderUpdate', {
        orderId,
        status: finalStatus,
        shipperId: user_id,
      });
    }
    
    if (orderRestaurantId) {
      io.to(`shop_${orderRestaurantId}`).emit('orderUpdate', {
        orderId,
        status: finalStatus,
        shipperId: user_id,
      });
    }

    console.log(`[SHIPPER ACCEPT] ✅ Shipper ${user_id} accepted order ${orderId}. Status: ${finalStatus}`);
    res.json({ success: true, orderId, status: finalStatus, shipper_id: user_id });
  } catch (err) {
    console.error('[SHIPPER ACCEPT] ❌ Error:', err);
    res.status(500).json({ error: 'failed_to_accept_order', message: err.message });
  }
});

// Cập nhật trạng thái đơn hàng (shipper)
app.patch('/shipper/orders/:id', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const user_role = req.user.role;
  const orderId = parseInt(req.params.id, 10);
  const { status, reason } = req.body || {};

  try {
    if (user_role !== 'SHIPPER' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    if (isNaN(orderId)) {
      return res.status(400).json({ error: 'invalid_order_id' });
    }

    if (!status) {
      return res.status(400).json({ error: 'status_required' });
    }

    // Kiểm tra đơn hàng có thuộc về shipper này không
    const orderResult = await pool.query(
      'SELECT id, status, shipper_id FROM orders WHERE id = $1',
      [orderId]
    );

    if (orderResult.rows.length === 0) {
      return res.status(404).json({ error: 'order_not_found' });
    }

    const order = orderResult.rows[0];
    if (order.shipper_id !== user_id && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'order_not_assigned_to_shipper' });
    }

    // ✅ FIX: Map frontend status to backend status
    const statusMap = {
      'picked_up': 'PICKED_UP',
      'shipping': 'SHIPPING',
      'delivering': 'DELIVERING',
      'delivered': 'DELIVERED',
      'canceled': 'CANCELED',
      'cancelled': 'CANCELED',
      'failed': 'CANCELED', // ✅ FIX: "failed" = hủy đơn, map sang CANCELED
    };
    
    const statusLower = (status || '').toLowerCase().trim();
    const newStatus = statusMap[statusLower] || status.toUpperCase();
    
    const allowedStatuses = ['PICKED_UP', 'SHIPPING', 'DELIVERING', 'DELIVERED', 'CANCELED'];
    if (!allowedStatuses.includes(newStatus)) {
      return res.status(400).json({ error: 'invalid_status', received: status, allowed: allowedStatuses });
    }

    // Cập nhật trạng thái
    await pool.query(
      'UPDATE orders SET status = $1, updated_at = NOW() WHERE id = $2',
      [newStatus, orderId]
    );

    // Thêm vào lịch sử
    try {
      const note = reason ? `Status updated by shipper: ${reason}` : `Status updated by shipper to ${newStatus}`;
      await pool.query(
        'INSERT INTO order_status_history (order_id, status, note) VALUES ($1, $2, $3)',
        [orderId, newStatus, note]
      );
    } catch (historyErr) {
      console.warn('[SHIPPER UPDATE STATUS] Could not insert into order_status_history:', historyErr.message);
    }

    // Nếu DELIVERED, set shipper available lại
    if (newStatus === 'DELIVERED') {
      await pool.query('UPDATE shippers SET available = true WHERE id = $1', [user_id]);
    }

    // Emit socket events
    const orderInfo = await pool.query('SELECT user_id FROM orders WHERE id = $1', [orderId]);
    const orderUserId = orderInfo.rows[0]?.user_id;
    
    if (orderUserId) {
      io.to(`order_${orderId}`).emit('statusUpdate', {
        orderId,
        status: newStatus,
      });
      io.to(`user_${orderUserId}`).emit('orderUpdate', {
        orderId,
        status: newStatus,
      });
    }

    console.log(`[SHIPPER UPDATE STATUS] ✅ Shipper ${user_id} updated order ${orderId} to ${newStatus}`);
    res.json({ success: true, orderId, status: newStatus });
  } catch (err) {
    console.error('[SHIPPER UPDATE STATUS] ❌ Error:', err);
    res.status(500).json({ error: 'failed_to_update_order_status', message: err.message });
  }
});

// Lấy chi tiết đơn hàng cho shipper
app.get('/shipper/orders/:id', requireAuth, async (req, res) => {
  const orderIdParam = req.params.id;
  const user_id = req.user.id;
  const user_role = req.user.role;

  try {
    if (user_role !== 'SHIPPER' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    const orderId = parseInt(orderIdParam, 10);
    if (isNaN(orderId)) {
      return res.status(400).json({ error: 'invalid_order_id' });
    }

    // Kiểm tra đơn hàng có thuộc về shipper này hoặc available không
    // ✅ FIX: Cho phép shipper xem đơn hàng nếu:
    // 1. Đơn hàng đã được gán cho shipper này
    // 2. HOẶC đơn hàng chưa có shipper (available)
    // 3. HOẶC đơn hàng có status READY (có thể nhận)
    let query = `
      SELECT 
        o.id as order_id,
        o.user_id,
        o.restaurant_id,
        o.status,
        o.total as total_amount,
        o.total,
        o.created_at,
        o.address,
        o.payment_method,
        o.shipper_id,
        r.name as restaurant_name,
        u.username as customer_name,
        s.vehicle_plate as shipper_vehicle_plate,
        u_shipper.phone as shipper_phone,
        u_shipper.username as shipper_name,
        u_shipper.email as shipper_email,
        s.lat as shipper_lat,
        s.lng as shipper_lng
      FROM orders o
      LEFT JOIN restaurants r ON r.id = o.restaurant_id
      LEFT JOIN users u ON u.id = o.user_id
      LEFT JOIN shippers s ON s.id = o.shipper_id
      LEFT JOIN users u_shipper ON u_shipper.id = o.shipper_id
      WHERE o.id = $1 AND (o.shipper_id = $2 OR o.shipper_id IS NULL OR o.status = 'READY')
    `;
    const params = [orderId, user_id];
    
    console.log('[SHIPPER ORDER DETAIL] Query:', query);
    console.log('[SHIPPER ORDER DETAIL] Params:', params);

    console.log(`[SHIPPER ORDER DETAIL] Querying order ${orderId} for shipper ${user_id}`);
    const result = await pool.query(query, params);
    console.log(`[SHIPPER ORDER DETAIL] Found ${result.rows.length} order(s)`);

    if (result.rows.length === 0) {
      console.log(`[SHIPPER ORDER DETAIL] Order ${orderId} not found or not accessible for shipper ${user_id}`);
      return res.status(404).json({ error: 'order_not_found', message: 'Order not found or not accessible' });
    }

    const order = result.rows[0];
    console.log('[SHIPPER ORDER DETAIL] Order data:', {
      order_id: order.order_id,
      status: order.status,
      shipper_id: order.shipper_id,
      address: order.address,
      total_amount: order.total_amount,
      total: order.total
    });

    // Lấy items của đơn hàng
    // ✅ FIX: Database schema thực tế là menu_item_id, quantity, unit_price, item_name
    let itemsResult;
    try {
      // Thử schema thực tế trước (menu_item_id, quantity, unit_price, item_name)
      itemsResult = await pool.query(
        'SELECT menu_item_id as product_id, quantity as qty, unit_price as price, item_name FROM order_items WHERE order_id = $1',
        [orderId]
      );
    } catch (err) {
      // Fallback: thử schema khác (product_id, qty, price)
      try {
        itemsResult = await pool.query(
          'SELECT product_id, qty, price FROM order_items WHERE order_id = $1',
          [orderId]
        );
      } catch (err2) {
        // Nếu cả hai đều lỗi, thử schema khác (product_id, quantity, price)
        try {
          itemsResult = await pool.query(
            'SELECT product_id, quantity as qty, price FROM order_items WHERE order_id = $1',
            [orderId]
          );
        } catch (err3) {
          console.warn('[SHIPPER ORDER DETAIL] Could not fetch order items:', err3.message);
          // Nếu cả ba đều lỗi, dùng empty result
          itemsResult = { rows: [] };
        }
      }
    }

    // ✅ FIX: Lấy tên sản phẩm từ products table
    const productIds = itemsResult.rows.map(row => row.product_id).filter(id => id != null);
    const productNamesMap = new Map();
    if (productIds.length > 0) {
      try {
        const productsResult = await pool.query(
          'SELECT id, name FROM products WHERE id = ANY($1::int[])',
          [productIds]
        );
        productsResult.rows.forEach(row => {
          productNamesMap.set(row.id, row.name);
        });
      } catch (err) {
        console.warn('[SHIPPER ORDER DETAIL] Could not fetch product names:', err.message);
      }
    }
    
    const mappedItems = itemsResult.rows.map(row => {
      const productId = row.product_id;
      const qty = parseInt(row.qty) || 1;
      const price = parseFloat(row.price) || 0;
      const lineTotal = price * qty; // ✅ FIX: Tính line_total để dùng cho total calculation
      // ✅ FIX: Ưu tiên dùng item_name từ database, sau đó mới dùng productNamesMap
      const productName = row.item_name || productNamesMap.get(productId) || `Món #${productId}`;
      return {
        product_id: productId,
        qty: qty,
        quantity: qty, // ✅ FIX: Thêm quantity để tương thích với code tính total
        price: price,
        line_total: lineTotal, // ✅ FIX: Thêm line_total để tính total chính xác
        name: productName,
      };
    });

    // Lấy lịch sử trạng thái
    let statusHistoryResult;
    try {
      statusHistoryResult = await pool.query(
        `SELECT status, note, created_at, id
        FROM order_status_history
        WHERE order_id = $1
        ORDER BY created_at ASC`,
        [orderId]
      );
    } catch (err) {
      // Nếu không có created_at, dùng id để sort
      try {
        statusHistoryResult = await pool.query(
          `SELECT status, note, id
          FROM order_status_history
          WHERE order_id = $1
          ORDER BY id ASC`,
          [orderId]
        );
      } catch (err2) {
        console.warn('[SHIPPER ORDER DETAIL] Could not fetch status history:', err2.message);
        statusHistoryResult = { rows: [] };
      }
    }

    // ✅ FIX: Lấy total (database chỉ có cột total, đã map thành total_amount)
    console.log('[SHIPPER ORDER DETAIL] Raw order.total:', order.total, 'order.total_amount:', order.total_amount);
    const orderTotal = parseFloat(order.total_amount || order.total || 0);
    console.log('[SHIPPER ORDER DETAIL] Parsed order total:', orderTotal, 'from:', { total_amount: order.total_amount, total: order.total });
    
    // ✅ FIX: Luôn tính lại total từ items để đảm bảo chính xác (dùng line_total đã tính sẵn)
    let calculatedTotal = 0;
    if (mappedItems.length > 0) {
      calculatedTotal = mappedItems.reduce((sum, item) => {
        // Ưu tiên dùng line_total (đã tính sẵn), nếu không có thì tính lại
        const lineTotal = parseFloat(item.line_total) || (parseFloat(item.price) * parseInt(item.qty || item.quantity) || 0);
        return sum + lineTotal;
      }, 0);
      if (Math.abs(calculatedTotal - orderTotal) > 0.01) {
        console.log('[SHIPPER ORDER DETAIL] Calculated total from items:', calculatedTotal, '(DB had:', orderTotal, ')');
      }
    } else {
      calculatedTotal = orderTotal;
    }
    
    // ✅ FIX: Thêm thông tin shipper vào response
    const shipperInfo = order.shipper_id ? {
      shipper_id: order.shipper_id,
      shipper_name: order.shipper_name || order.shipper_email || 'N/A',
      shipper_email: order.shipper_email || null,
      shipper_phone: order.shipper_phone || null,
      vehicle_plate: order.shipper_vehicle_plate || null,
      shipper_lat: order.shipper_lat ? parseFloat(order.shipper_lat) : null,
      shipper_lng: order.shipper_lng ? parseFloat(order.shipper_lng) : null,
    } : null;

    res.json({
      order: {
        order_id: order.order_id,
        status: order.status,
        total: calculatedTotal,
        total_amount: calculatedTotal,
        address: order.address || null,
        payment_method: order.payment_method,
        created_at: order.created_at,
        restaurant_name: order.restaurant_name,
        restaurant_id: order.restaurant_id,
        user_id: order.user_id,
        shipper_id: order.shipper_id,
      },
      shipper: shipperInfo,
      items: mappedItems,
      history: statusHistoryResult.rows,
    });
  } catch (err) {
    console.error('[SHIPPER ORDER DETAIL] ❌ Error:', err);
    console.error('[SHIPPER ORDER DETAIL] Error stack:', err.stack);
    res.status(500).json({ error: 'failed_to_fetch_order', message: err.message, stack: err.stack });
  }
});

// Lấy doanh thu shipper
app.get('/shipper/revenue', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const user_role = req.user.role;

  try {
    if (user_role !== 'SHIPPER' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    // Lấy doanh thu hôm nay (đơn đã giao hôm nay)
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    const todayRevenueResult = await pool.query(
      `SELECT 
        COUNT(*) as today_orders,
        COALESCE(SUM(o.total), 0) as today_revenue
      FROM orders o
      WHERE o.shipper_id = $1 
        AND o.status = 'DELIVERED'
        AND o.updated_at >= $2 
        AND o.updated_at < $3`,
      [user_id, today, tomorrow]
    );

    // Lấy tổng doanh thu và số đơn đã giao
    const totalRevenueResult = await pool.query(
      `SELECT 
        COUNT(*) as total_orders,
        COALESCE(SUM(o.total), 0) as total_revenue
      FROM orders o
      WHERE o.shipper_id = $1 
        AND o.status = 'DELIVERED'`,
      [user_id]
    );

    const todayData = todayRevenueResult.rows[0];
    const totalData = totalRevenueResult.rows[0];

    res.json({
      today_revenue: parseFloat(todayData.today_revenue) || 0,
      today_orders: parseInt(todayData.today_orders) || 0,
      total_revenue: parseFloat(totalData.total_revenue) || 0,
      total_orders: parseInt(totalData.total_orders) || 0,
    });
  } catch (err) {
    console.error('[SHIPPER REVENUE] ❌ Error:', err);
    res.status(500).json({ error: 'failed_to_fetch_revenue', message: err.message });
  }
});

// ---------- Shipper Profile ----------
// Lấy thông tin profile shipper
app.get('/shipper/profile', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const user_role = req.user.role;

  try {
    if (user_role !== 'SHIPPER' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    const result = await pool.query(
      `SELECT 
        u.id,
        u.username,
        u.email,
        u.phone,
        s.available,
        s.vehicle_plate,
        s.lat,
        s.lng
      FROM users u
      LEFT JOIN shippers s ON s.id = u.id
      WHERE u.id = $1`,
      [user_id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'shipper_not_found' });
    }

    const shipper = result.rows[0];
    res.json({
      id: shipper.id,
      username: shipper.username,
      email: shipper.email,
      phone: shipper.phone || null,
      available: shipper.available,
      vehicle_plate: shipper.vehicle_plate || null,
      lat: shipper.lat ? parseFloat(shipper.lat) : null,
      lng: shipper.lng ? parseFloat(shipper.lng) : null,
    });
  } catch (err) {
    console.error('[SHIPPER PROFILE] ❌ Error:', err);
    res.status(500).json({ error: 'failed_to_fetch_profile', message: err.message });
  }
});

// Cập nhật thông tin profile shipper
app.patch('/shipper/profile', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const user_role = req.user.role;
  const { username, email, phone, vehicle_plate } = req.body || {};

  console.log('[SHIPPER PROFILE UPDATE] Request from user:', user_id, 'role:', user_role);
  console.log('[SHIPPER PROFILE UPDATE] Body:', { username, email, phone, vehicle_plate });

  try {
    if (user_role !== 'SHIPPER' && user_role !== 'ADMIN') {
      console.log('[SHIPPER PROFILE UPDATE] ❌ Forbidden: user role is', user_role);
      return res.status(403).json({ error: 'forbidden' });
    }

    // Cập nhật username trong users table
    if (username !== undefined && username !== null && username !== '') {
      console.log('[SHIPPER PROFILE UPDATE] Updating username to:', username);
      // Kiểm tra username đã tồn tại chưa (trừ chính user này)
      const checkUsername = await pool.query(
        'SELECT id FROM users WHERE username = $1 AND id != $2',
        [username, user_id]
      );
      if (checkUsername.rows.length > 0) {
        console.log('[SHIPPER PROFILE UPDATE] ❌ Username already exists');
        return res.status(400).json({ error: 'username_already_exists' });
      }
      await pool.query(
        'UPDATE users SET username = $1 WHERE id = $2',
        [username, user_id]
      );
      console.log('[SHIPPER PROFILE UPDATE] ✅ Username updated');
    }

    // Cập nhật email trong users table
    if (email !== undefined && email !== null && email !== '') {
      console.log('[SHIPPER PROFILE UPDATE] Updating email to:', email);
      // Kiểm tra email đã tồn tại chưa (trừ chính user này)
      const checkEmail = await pool.query(
        'SELECT id FROM users WHERE email = $1 AND id != $2',
        [email, user_id]
      );
      if (checkEmail.rows.length > 0) {
        console.log('[SHIPPER PROFILE UPDATE] ❌ Email already exists');
        return res.status(400).json({ error: 'email_already_exists' });
      }
      await pool.query(
        'UPDATE users SET email = $1 WHERE id = $2',
        [email, user_id]
      );
      console.log('[SHIPPER PROFILE UPDATE] ✅ Email updated');
    }

    // Cập nhật phone trong users table
    if (phone !== undefined) {
      console.log('[SHIPPER PROFILE UPDATE] Updating phone to:', phone);
      await pool.query(
        'UPDATE users SET phone = $1 WHERE id = $2',
        [phone, user_id]
      );
      console.log('[SHIPPER PROFILE UPDATE] ✅ Phone updated');
    }

    // Cập nhật vehicle_plate trong shippers table
    if (vehicle_plate !== undefined) {
      console.log('[SHIPPER PROFILE UPDATE] Updating vehicle_plate to:', vehicle_plate);
      await pool.query(
        'UPDATE shippers SET vehicle_plate = $1 WHERE id = $2',
        [vehicle_plate, user_id]
      );
      console.log('[SHIPPER PROFILE UPDATE] ✅ Vehicle plate updated');
    }

    // Lấy lại thông tin đã cập nhật
    const result = await pool.query(
      `SELECT 
        u.id,
        u.username,
        u.email,
        u.phone,
        s.available,
        s.vehicle_plate,
        s.lat,
        s.lng
      FROM users u
      LEFT JOIN shippers s ON s.id = u.id
      WHERE u.id = $1`,
      [user_id]
    );

    const shipper = result.rows[0];
    console.log('[SHIPPER PROFILE UPDATE] ✅ Successfully updated profile for user:', user_id);
    res.json({
      id: shipper.id,
      username: shipper.username,
      email: shipper.email,
      phone: shipper.phone || null,
      available: shipper.available,
      vehicle_plate: shipper.vehicle_plate || null,
      lat: shipper.lat ? parseFloat(shipper.lat) : null,
      lng: shipper.lng ? parseFloat(shipper.lng) : null,
    });
  } catch (err) {
    console.error('[SHIPPER PROFILE UPDATE] ❌ Error:', err);
    console.error('[SHIPPER PROFILE UPDATE] Error stack:', err.stack);
    res.status(500).json({ error: 'failed_to_update_profile', message: err.message });
  }
});

// Cập nhật vị trí shipper
app.post('/shipper/location', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const user_role = req.user.role;
  const { lat, lng, accuracy } = req.body || {};

  try {
    if (user_role !== 'SHIPPER' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    if (lat == null || lng == null) {
      return res.status(400).json({ error: 'lat_lng_required' });
    }

    // Cập nhật vị trí shipper
    await pool.query(
      'UPDATE shippers SET lat = $1, lng = $2, updated_at = NOW() WHERE id = $3',
      [parseFloat(lat), parseFloat(lng), user_id]
    );

    // Emit socket event để customer có thể theo dõi
    io.to(`shipper_${user_id}`).emit('shipper:location', {
      shipperId: user_id,
      lat: parseFloat(lat),
      lng: parseFloat(lng),
      accuracy: accuracy ? parseFloat(accuracy) : null,
    });

    console.log(`[SHIPPER LOCATION] ✅ Updated location for shipper ${user_id}: ${lat}, ${lng}`);
    res.json({ success: true, lat: parseFloat(lat), lng: parseFloat(lng) });
  } catch (err) {
    console.error('[SHIPPER LOCATION] ❌ Error:', err);
    res.status(500).json({ error: 'failed_to_update_location', message: err.message });
  }
});

// ---------- Merchant Revenue/Stats ----------
app.get('/merchant/revenue', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const user_role = req.user.role;
  const { start_date, end_date } = req.query;

  try {
    if (user_role !== 'MERCHANT' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    // Lấy restaurant_id của merchant
    let restaurantId;
    if (user_role === 'ADMIN') {
      restaurantId = parseInt(req.query.restaurant_id, 10);
      if (isNaN(restaurantId)) {
        return res.status(400).json({ error: 'restaurant_id_required_for_admin' });
      }
    } else {
      const restaurantResult = await pool.query(
        'SELECT id FROM restaurants WHERE id = $1 OR id IN (SELECT restaurant_id FROM user_restaurants WHERE user_id = $1)',
        [user_id]
      );
      if (restaurantResult.rows.length === 0) {
        restaurantId = user_id;
      } else {
        restaurantId = restaurantResult.rows[0].id;
      }
    }

    let query = `
      SELECT 
        COUNT(*) as total_orders,
        -- ✅ FIX: Doanh thu chỉ tính từ đơn DELIVERED (đã giao thành công)
        -- Không tính READY (chỉ mới làm xong, chưa giao)
        COALESCE(SUM(CASE WHEN status = 'DELIVERED' THEN COALESCE(total_amount, total, 0) ELSE 0 END), 0) as total_revenue,
        COALESCE(SUM(CASE WHEN status = 'DELIVERED' THEN COALESCE(total_amount, total, 0) ELSE 0 END), 0) as completed_revenue,
        COALESCE(SUM(CASE WHEN status = 'PENDING' OR status = 'CONFIRMED' THEN 1 ELSE 0 END), 0) as pending_orders,
        COALESCE(SUM(CASE WHEN status = 'PREPARING' OR status = 'COOKING' THEN 1 ELSE 0 END), 0) as preparing_orders,
        COALESCE(SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END), 0) as completed_orders
      FROM orders
      WHERE restaurant_id = $1
    `;
    const params = [restaurantId];

    if (start_date) {
      query += ' AND created_at >= $' + (params.length + 1);
      params.push(start_date);
    }
    if (end_date) {
      query += ' AND created_at <= $' + (params.length + 1);
      params.push(end_date);
    }

    const result = await pool.query(query, params);
    res.json(result.rows[0]);
  } catch (err) {
    console.error('merchant revenue error', err);
    res.status(500).json({ error: 'failed_to_fetch_revenue' });
  }
});

// =========================================================
// REVIEW & RATING (UC-11)
// =========================================================

// Tạo review
app.post('/reviews', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const { order_id, order_rating, merchant_rating, shipper_rating, comment, menu_item_reviews } = req.body;

  try {
    // Kiểm tra đơn hàng
    const orderResult = await pool.query(
      'SELECT id, status, shipper_id FROM orders WHERE id = $1 AND user_id = $2',
      [order_id, user_id]
    );

    if (orderResult.rows.length === 0) {
      return res.status(404).json({ error: 'order_not_found' });
    }

    const order = orderResult.rows[0];
    if (order.status !== 'DELIVERED') {
      return res.status(400).json({ error: 'can_only_review_delivered_orders' });
    }

    // Kiểm tra đã review chưa
    const existingReview = await pool.query(
      'SELECT id FROM reviews WHERE order_id = $1 AND customer_id = $2',
      [order_id, user_id]
    );

    if (existingReview.rows.length > 0) {
      return res.status(400).json({ error: 'already_reviewed' });
    }

    // Tạo review
    const reviewResult = await pool.query(
      `INSERT INTO reviews (order_id, customer_id, order_rating, merchant_rating, shipper_rating, comment, created_at)
       VALUES ($1, $2, $3, $4, $5, $6, NOW()) RETURNING id`,
      [order_id, user_id, order_rating || 5, merchant_rating, shipper_rating || (order.shipper_id ? shipper_rating : null), comment || '']
    );

    const reviewId = reviewResult.rows[0].id;

    // Tạo menu item reviews
    if (menu_item_reviews && Array.isArray(menu_item_reviews)) {
      for (const itemReview of menu_item_reviews) {
        await pool.query(
          `INSERT INTO menu_item_reviews (review_id, order_item_id, rating, comment, created_at)
           VALUES ($1, $2, $3, $4, NOW())`,
          [reviewId, itemReview.order_item_id, itemReview.rating || 5, itemReview.comment || '']
        );
      }
    }

    res.json({ id: reviewId, order_id, message: 'review_created' });
  } catch (err) {
    console.error('create review error', err);
    res.status(500).json({ error: 'failed_to_create_review' });
  }
});

// =========================================================
// COMPLAINT & FEEDBACK (UC-13)
// =========================================================

// Tạo complaint
app.post('/complaints', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const { order_id, complaint_type, title, description } = req.body;

  try {
    const orderResult = await pool.query(
      'SELECT id FROM orders WHERE id = $1 AND user_id = $2',
      [order_id, user_id]
    );

    if (orderResult.rows.length === 0) {
      return res.status(404).json({ error: 'order_not_found' });
    }

    const result = await pool.query(
      `INSERT INTO complaints (order_id, customer_id, complaint_type, title, description, status, created_at)
       VALUES ($1, $2, $3, $4, $5, 'PENDING', NOW()) RETURNING id`,
      [order_id, user_id, complaint_type || 'OTHER', title, description]
    );

    res.json({ id: result.rows[0].id, message: 'complaint_created' });
  } catch (err) {
    console.error('create complaint error', err);
    res.status(500).json({ error: 'failed_to_create_complaint' });
  }
});

// Danh sách complaints
app.get('/complaints', requireAuth, async (req, res) => {
  const user_id = req.user.id;
  const user_role = req.user.role;

  try {
    let query;
    let params = [];

    if (user_role === 'USER' || user_role === 'CUSTOMER') {
      query = 'SELECT * FROM complaints WHERE customer_id = $1 ORDER BY created_at DESC';
      params = [user_id];
    } else if (user_role === 'MERCHANT') {
      query = `
        SELECT c.* FROM complaints c
        JOIN orders o ON o.id = c.order_id
        JOIN restaurants r ON r.id = o.restaurant_id
        WHERE r.id IN (SELECT restaurant_id FROM user_restaurants WHERE user_id = $1)
        ORDER BY c.created_at DESC
      `;
      params = [user_id];
    } else if (user_role === 'ADMIN') {
      query = 'SELECT * FROM complaints ORDER BY created_at DESC';
    } else {
      return res.status(403).json({ error: 'forbidden' });
    }

    const result = await pool.query(query, params);
    res.json(result.rows);
  } catch (err) {
    console.error('get complaints error', err);
    res.status(500).json({ error: 'failed_to_get_complaints' });
  }
});

// Phản hồi complaint
app.post('/complaints/:id/respond', requireAuth, async (req, res) => {
  const complaintId = parseInt(req.params.id, 10);
  const user_id = req.user.id;
  const user_role = req.user.role;
  const { response, status } = req.body;

  try {
    if (user_role !== 'MERCHANT' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    const updateQuery = `
      UPDATE complaints
      SET response = $1, status = $2, handled_by = $3, resolved_at = CASE WHEN $2 = 'RESOLVED' THEN NOW() ELSE resolved_at END, updated_at = NOW()
      WHERE id = $4
      RETURNING *
    `;
    const result = await pool.query(updateQuery, [response, status, user_id, complaintId]);

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'complaint_not_found' });
    }

    res.json(result.rows[0]);
  } catch (err) {
    console.error('respond complaint error', err);
    res.status(500).json({ error: 'failed_to_respond_complaint' });
  }
});

// =========================================================
// MERCHANT: QUẢN LÝ KHO (UC-04)
// =========================================================

app.post('/inventory/:id/adjust_stock', requireAuth, async (req, res) => {
  const menuItemId = parseInt(req.params.id, 10);
  const user_id = req.user.id;
  const user_role = req.user.role;
  const { quantity, type } = req.body;

  try {
    if (user_role !== 'MERCHANT' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    const menuResult = await pool.query('SELECT id, stock, restaurant_id FROM menu_items WHERE id = $1', [menuItemId]);
    if (menuResult.rows.length === 0) {
      return res.status(404).json({ error: 'menu_item_not_found' });
    }

    const menuItem = menuResult.rows[0];
    let newStock = menuItem.stock;

    if (type === 'IN') {
      newStock += Math.abs(quantity);
    } else if (type === 'OUT') {
      newStock = Math.max(0, newStock - Math.abs(quantity));
    } else {
      newStock = Math.max(0, quantity);
    }

    await pool.query('UPDATE menu_items SET stock = $1 WHERE id = $2', [newStock, menuItemId]);

    res.json({ id: menuItemId, stock: newStock, message: 'stock_updated' });
  } catch (err) {
    console.error('adjust stock error', err);
    res.status(500).json({ error: 'failed_to_adjust_stock' });
  }
});

// =========================================================
// MERCHANT: XỬ LÝ THIẾU KHO (UC-12)
// =========================================================

app.post('/merchant/orders/:id/handle_out_of_stock', requireAuth, async (req, res) => {
  const orderId = parseInt(req.params.id, 10);
  const user_id = req.user.id;
  const user_role = req.user.role;
  const { action, substitutions, reductions, reason } = req.body;

  try {
    if (user_role !== 'MERCHANT' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    const orderResult = await pool.query('SELECT id, restaurant_id FROM orders WHERE id = $1', [orderId]);
    if (orderResult.rows.length === 0) {
      return res.status(404).json({ error: 'order_not_found' });
    }

    const client = await pool.connect();
    try {
      await client.query('BEGIN');

      if (action === 'SUBSTITUTE' && substitutions) {
        for (const sub of substitutions) {
          const newMenuResult = await client.query('SELECT name, price FROM menu_items WHERE id = $1', [sub.new_menu_item_id]);
          if (newMenuResult.rows.length > 0) {
            const newMenu = newMenuResult.rows[0];
            await client.query(
              'UPDATE order_items SET menu_item_id = $1, name_snapshot = $2, price_snapshot = $3, line_total = $3 * quantity WHERE id = $4',
              [sub.new_menu_item_id, newMenu.name, newMenu.price, sub.order_item_id]
            );
          }
        }
      } else if (action === 'REDUCE' && reductions) {
        for (const red of reductions) {
          await client.query(
            'UPDATE order_items SET quantity = $1, line_total = price_snapshot * $1 WHERE id = $2',
            [Math.max(1, red.new_quantity), red.order_item_id]
          );
        }
      } else if (action === 'CANCEL') {
        await client.query("UPDATE orders SET status = 'CANCELED', payment_status = CASE WHEN payment_status = 'PAID' THEN 'REFUNDED' ELSE payment_status END WHERE id = $1", [orderId]);
        await client.query('COMMIT');
        return res.json({ id: orderId, status: 'CANCELED', message: 'order_canceled_due_to_out_of_stock' });
      }

      // Tính lại tổng tiền
      const totalResult = await client.query('SELECT SUM(line_total) as total FROM order_items WHERE order_id = $1', [orderId]);
      const total = totalResult.rows[0].total || 0;
      await client.query('UPDATE orders SET total_amount = $1 WHERE id = $2', [total, orderId]);

      await client.query('COMMIT');
      res.json({ id: orderId, total_amount: total, message: `handled_out_of_stock_with_${action}` });
    } catch (err) {
      await client.query('ROLLBACK');
      throw err;
    } finally {
      client.release();
    }
  } catch (err) {
    console.error('handle out of stock error', err);
    res.status(500).json({ error: 'failed_to_handle_out_of_stock' });
  }
});

// =========================================================
// MERCHANT: REFUND (UC-14)
// =========================================================

app.post('/merchant/orders/:id/refund', requireAuth, async (req, res) => {
  const orderId = parseInt(req.params.id, 10);
  const user_id = req.user.id;
  const user_role = req.user.role;
  const { amount, reason } = req.body;

  try {
    if (user_role !== 'MERCHANT' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    const orderResult = await pool.query('SELECT id, total_amount, payment_status FROM orders WHERE id = $1', [orderId]);
    if (orderResult.rows.length === 0) {
      return res.status(404).json({ error: 'order_not_found' });
    }

    const order = orderResult.rows[0];
    if (order.payment_status !== 'PAID') {
      return res.status(400).json({ error: 'can_only_refund_paid_orders' });
    }

    const refundAmount = amount ? Math.min(amount, order.total_amount) : order.total_amount;

    await pool.query(
      "UPDATE orders SET payment_status = 'REFUNDED' WHERE id = $1",
      [orderId]
    );

    res.json({ id: orderId, refund_amount: refundAmount, payment_status: 'REFUNDED', message: 'refund_processed' });
  } catch (err) {
    console.error('refund error', err);
    res.status(500).json({ error: 'failed_to_process_refund' });
  }
});

// =========================================================
// SHIPPER: XỬ LÝ VẤN ĐỀ
// =========================================================

app.post('/shipper/orders/:id/report_issue', requireAuth, async (req, res) => {
  const orderId = parseInt(req.params.id, 10);
  const user_id = req.user.id;
  const user_role = req.user.role;
  const { issue_type, reason } = req.body;

  try {
    if (user_role !== 'SHIPPER' && user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    const orderResult = await pool.query('SELECT id, status, note FROM orders WHERE id = $1 AND shipper_id = $2', [orderId, user_id]);
    if (orderResult.rows.length === 0) {
      return res.status(404).json({ error: 'order_not_found_or_not_assigned' });
    }

    const order = orderResult.rows[0];
    const newNote = `${order.note || ''}\n[Shipper Issue]: ${reason}`.trim();

    await pool.query(
      "UPDATE orders SET status = 'CANCELED', note = $1 WHERE id = $2",
      [newNote, orderId]
    );

    res.json({ id: orderId, status: 'CANCELED', message: `issue_reported_${issue_type}` });
  } catch (err) {
    console.error('report issue error', err);
    res.status(500).json({ error: 'failed_to_report_issue' });
  }
});

// =========================================================
// ADMIN: QUẢN LÝ USER & ROLE (UC-09)
// =========================================================

// ✅ REMOVED: Duplicate /admin/users endpoint (keeping the one at line 1492 which returns { users: [...] })

app.patch('/admin/users/:id/update_role', requireAuth, async (req, res) => {
  const userId = parseInt(req.params.id, 10);
  const user_role = req.user.role;
  const { role } = req.body;

  try {
    if (user_role !== 'ADMIN') {
      return res.status(403).json({ error: 'forbidden' });
    }

    if (!['USER', 'CUSTOMER', 'MERCHANT', 'SHIPPER', 'ADMIN'].includes(role)) {
      return res.status(400).json({ error: 'invalid_role' });
    }

    await pool.query('UPDATE users SET role = $1 WHERE id = $2', [role, userId]);

    res.json({ id: userId, role, message: 'role_updated' });
  } catch (err) {
    console.error('update role error', err);
    res.status(500).json({ error: 'failed_to_update_role' });
  }
});

// Start server
const PORT = process.env.PORT || 8000;
server.listen(PORT, '0.0.0.0', () => {
  console.log(`Fast Food API Service is running on port ${PORT}.`);
});
