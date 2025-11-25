#!/usr/bin/env node
/**
 * Cleanup old revoked JWTs and expired refresh tokens.
 * - REVOCATION_TTL_DAYS: số ngày giữ lại bản ghi token đã thu hồi.
 *   Nếu 0 hoặc giá trị không hợp lệ -> bỏ qua xoá revoked_tokens.
 * - Luôn xoá refresh_tokens đã hết hạn.
 */
const { Pool } = require('pg');
require('dotenv').config();

const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5432', 10),
  database: process.env.DB_NAME || 'fastfood',
  user: process.env.DB_USER || 'app',
  password: process.env.DB_PASSWORD || '123456',
});

async function cleanup() {
  const ttlDaysRaw = process.env.REVOCATION_TTL_DAYS;
  // TTL hợp lệ: số nguyên >= 0, mặc định 7
  let ttlDays = Number.isFinite(Number(ttlDaysRaw))
    ? parseInt(ttlDaysRaw, 10)
    : 7;
  if (ttlDays < 0) ttlDays = 0;

  try {
    console.log('[cleanup] starting…');

    // 1) Xoá revoked_tokens theo TTL (nếu TTL > 0)
    if (ttlDays > 0) {
      const delRevoked = await pool.query(
        `
        DELETE FROM revoked_tokens
        WHERE revoked_at < (NOW() - ($1::int * INTERVAL '1 day'))
        `,
        [ttlDays]
      );
      console.log(
        `[cleanup] revoked_tokens deleted: ${delRevoked.rowCount} (TTL=${ttlDays} days)`
      );
    } else {
      console.log(
        `[cleanup] REVOCATION_TTL_DAYS=${ttlDays} => skip deleting revoked_tokens`
      );
    }

    // 2) Xoá refresh_tokens đã hết hạn
    const delRefresh = await pool.query(
      `DELETE FROM refresh_tokens WHERE expires_at < NOW()`
    );
    console.log(
      `[cleanup] refresh_tokens deleted (expired): ${delRefresh.rowCount}`
    );

    console.log('[cleanup] done ✅');
    process.exit(0);
  } catch (e) {
    console.error('[cleanup] error:', e);
    process.exit(2);
  } finally {
    // đóng pool gọn gàng
    try {
      await pool.end();
    } catch {}
  }
}

cleanup();
