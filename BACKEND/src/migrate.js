import dotenv from 'dotenv';
import pg from 'pg';
import { readdir, readFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
dotenv.config();
if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL não configurada.');
const { Pool } = pg;
const pool = new Pool({ connectionString: process.env.DATABASE_URL, ssl: process.env.DATABASE_SSL === 'true' ? { rejectUnauthorized: true } : undefined });
const dir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../db/migrations');
try {
  await pool.query('SELECT pg_advisory_lock(7319042026)');
  await pool.query('CREATE TABLE IF NOT EXISTS schema_migrations (version VARCHAR(100) PRIMARY KEY, checksum CHAR(64) NOT NULL, applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW())');
  const files = (await readdir(dir)).filter(x => /^\d+_[a-z0-9_-]+\.sql$/.test(x)).sort();
  for (const name of files) {
    const sql = await readFile(path.join(dir, name), 'utf8');
    const checksum = createHash('sha256').update(sql).digest('hex');
    const existing = await pool.query('SELECT checksum FROM schema_migrations WHERE version=$1', [name]);
    if (existing.rowCount) {
      if (existing.rows[0].checksum !== checksum) throw new Error(`Migração já aplicada foi alterada: ${name}. Crie uma nova migração numerada.`);
      console.log(`Já aplicada: ${name}`); continue;
    }
    await pool.query('BEGIN');
    try {
      await pool.query(sql);
      await pool.query('INSERT INTO schema_migrations(version,checksum) VALUES($1,$2)', [name, checksum]);
      await pool.query('COMMIT'); console.log(`Aplicada: ${name}`);
    } catch (e) { await pool.query('ROLLBACK'); throw e; }
  }
} finally {
  try { await pool.query('SELECT pg_advisory_unlock(7319042026)'); } catch {}
  await pool.end();
}
