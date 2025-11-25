/**
 * Script để kiểm tra RLS và permissions
 * Usage: node check_rls_and_permissions.js
 */

require('dotenv').config({ override: true });
const { Pool } = require('pg');

const DB_HOST = process.env.DB_HOST || process.env.PGHOST || process.env.POSTGRES_HOST || '127.0.0.1';
const DB_PORT = parseInt(process.env.DB_PORT || process.env.PGPORT || process.env.POSTGRES_PORT || '5432', 10);
const DB_NAME = process.env.DB_NAME || process.env.PGDATABASE || process.env.POSTGRES_DB || 'fastfood';
const DB_USER = process.env.DB_USER || process.env.PGUSER || process.env.POSTGRES_USER || 'app';
const DB_PASS = process.env.DB_PASSWORD || process.env.PGPASSWORD || process.env.POSTGRES_PASSWORD || '123456';

const pool = new Pool({
  host: DB_HOST,
  port: DB_PORT,
  database: DB_NAME,
  user: DB_USER,
  password: DB_PASS,
});

async function checkRLSAndPermissions() {
  try {
    console.log('=== Checking RLS and Permissions ===\n');
    console.log(`Connection: ${DB_USER}@${DB_NAME}@${DB_HOST}:${DB_PORT}\n`);

    // Check if RLS is enabled
    const rlsCheck = await pool.query(`
      SELECT tablename, rowsecurity 
      FROM pg_tables 
      WHERE schemaname = 'public' 
      AND tablename = 'users'
    `);
    console.log('RLS Status for users table:');
    if (rlsCheck.rows.length > 0) {
      console.log(`  - RLS Enabled: ${rlsCheck.rows[0].rowsecurity}`);
    } else {
      console.log('  - Table not found');
    }
    console.log('');

    // Check RLS policies
    const policiesCheck = await pool.query(`
      SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual, with_check
      FROM pg_policies 
      WHERE schemaname = 'public' 
      AND tablename = 'users'
    `);
    console.log('RLS Policies for users table:');
    if (policiesCheck.rows.length > 0) {
      policiesCheck.rows.forEach(policy => {
        console.log(`  - Policy: ${policy.policyname}`);
        console.log(`    Roles: ${policy.roles}`);
        console.log(`    Command: ${policy.cmd}`);
      });
    } else {
      console.log('  - No RLS policies found');
    }
    console.log('');

    // Check table owner
    const ownerCheck = await pool.query(`
      SELECT tablename, tableowner 
      FROM pg_tables 
      WHERE schemaname = 'public' 
      AND tablename = 'users'
    `);
    console.log('Table Owner:');
    if (ownerCheck.rows.length > 0) {
      console.log(`  - Owner: ${ownerCheck.rows[0].tableowner}`);
    }
    console.log('');

    // Check permissions
    const permCheck = await pool.query(`
      SELECT grantee, privilege_type 
      FROM information_schema.role_table_grants 
      WHERE table_schema = 'public' 
      AND table_name = 'users'
      ORDER BY grantee, privilege_type
    `);
    console.log('Table Permissions:');
    permCheck.rows.forEach(perm => {
      console.log(`  - ${perm.grantee}: ${perm.privilege_type}`);
    });
    console.log('');

    // Check search_path
    const searchPathCheck = await pool.query('SHOW search_path');
    console.log(`Search Path: ${searchPathCheck.rows[0].search_path}\n`);

    // Count users with different methods
    console.log('User Counts:');
    const count1 = await pool.query('SELECT COUNT(*) as count FROM users');
    console.log(`  - SELECT COUNT(*): ${count1.rows[0].count}`);
    
    const count2 = await pool.query('SELECT COUNT(*) as count FROM public.users');
    console.log(`  - SELECT COUNT(*) FROM public.users: ${count2.rows[0].count}`);

    // Try to see all users (including those that might be hidden)
    const allUsers = await pool.query(`
      SELECT id, username, email, role, created_at 
      FROM users 
      ORDER BY id DESC 
      LIMIT 10
    `);
    console.log(`\nLast 10 users by ID:`);
    allUsers.rows.forEach((u, i) => {
      console.log(`  ${i + 1}. ID: ${u.id}, Email: ${u.email}, Created: ${u.created_at}`);
    });

    await pool.end();
    process.exit(0);
  } catch (err) {
    console.error('Error:', err);
    await pool.end();
    process.exit(1);
  }
}

checkRLSAndPermissions();

