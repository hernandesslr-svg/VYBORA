import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import pg from 'pg';
import dotenv from 'dotenv';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { createHash, randomUUID } from 'node:crypto';
import { createUploadUrl, createDownloadUrl, inspectObject } from './storage.js';

dotenv.config();
const { Pool } = pg;
const app = express();
const port = Number(process.env.PORT || 8080);
const serverDir = path.dirname(fileURLToPath(import.meta.url));
const remoteUiDir = path.resolve(serverDir, '../public/remote-ui');
const jwtSecret = process.env.JWT_SECRET || '';
if (jwtSecret.length < 32) throw new Error('JWT_SECRET precisa ter pelo menos 32 caracteres aleatórios.');
if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL não configurada.');
const caCertificate = process.env.DATABASE_SSL === 'true'
  ? await readFile(new URL('../certs/supabase-root.crt', import.meta.url), 'utf8')
  : undefined;
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: caCertificate ? { ca: caCertificate, rejectUnauthorized: true } : undefined,
  max: 15,
  idleTimeoutMillis: 30000
});
app.set('trust proxy', 1); // Railway fica atrás de proxy; necessário para rate-limit identificar o cliente corretamente.
app.disable('x-powered-by');
app.use(helmet({ contentSecurityPolicy: { directives: { 'img-src': ["'self'", 'data:', 'https:'], 'media-src': ["'self'", 'https:'], 'connect-src': ["'self'"], 'frame-ancestors': ["'none'"] } } }));
app.use(cors({ origin: process.env.CORS_ORIGIN === '*' ? true : (process.env.CORS_ORIGIN || '').split(',').filter(Boolean), credentials: false }));
app.use(express.json({ limit: '64kb' }));
app.use('/api/auth', rateLimit({ windowMs: 15 * 60 * 1000, limit: 30, standardHeaders: true, legacyHeaders: false }));
const asyncRoute = fn => (req,res,next) => Promise.resolve(fn(req,res,next)).catch(next);
const tokenFor = user => jwt.sign({ sub: user.id }, jwtSecret, { expiresIn: '7d', issuer: 'vybora-api', audience: 'vybora-app' });
const safeUser = user => ({ id: user.id, username: user.username, email: user.email, displayName: user.display_name, bio: user.bio, avatarUrl: user.avatar_url, verified: user.is_verified, createdAt: user.created_at });
const auth = asyncRoute(async (req,res,next) => {
  const value = req.get('authorization') || '';
  if (!value.startsWith('Bearer ')) return res.status(401).json({ error: 'Faça login para continuar.' });
  let payload;
  try {
    payload = jwt.verify(value.slice(7), jwtSecret, { issuer: 'vybora-api', audience: 'vybora-app' });
  } catch {
    return res.status(401).json({ error: 'Sessão inválida ou expirada.' });
  }
  const { rows } = await pool.query('SELECT id, username, email, display_name, bio, avatar_url, is_verified, created_at FROM users WHERE id=$1', [payload.sub]);
  if (!rows[0]) return res.status(401).json({ error: 'Sessão inválida.' });
  req.user = rows[0]; next();
});
app.get('/health', asyncRoute(async (_req,res) => { await pool.query('SELECT 1'); res.json({ status: 'ok', service: 'vybora-api', database: 'connected' }); }));

// Configuração pública remota do aplicativo. Nunca incluir segredos neste endpoint.
app.get('/api/app-config', (_req, res) => {
  res.set('Cache-Control', 'public, max-age=60, stale-while-revalidate=300');
  res.json({
    configVersion: Number(process.env.APP_CONFIG_VERSION || 1),
    maintenanceMode: process.env.APP_MAINTENANCE_MODE === 'true',
    maintenanceMessage: String(process.env.APP_MAINTENANCE_MESSAGE || '').slice(0, 180),
    feedEnabled: process.env.APP_FEED_ENABLED !== 'false',
    publishingEnabled: process.env.APP_PUBLISHING_ENABLED !== 'false',
    searchEnabled: process.env.APP_SEARCH_ENABLED !== 'false',
    messagesEnabled: process.env.APP_MESSAGES_ENABLED !== 'false',
    livesEnabled: process.env.APP_LIVES_ENABLED === 'true',
    announcement: String(process.env.APP_ANNOUNCEMENT || '').slice(0, 180),
    minSupportedVersionCode: Math.max(1, Number(process.env.APP_MIN_SUPPORTED_VERSION_CODE || 1))
  });
});


// Manifesto público da atualização do APK nativo (separado do OTA da interface web).
// A release precisa conter um APK assinado com a mesma chave da instalação existente.
app.get('/api/app-update', asyncRoute(async (_req, res) => {
  const releaseResponse = await fetch('https://api.github.com/repos/hernandesslr-svg/VYBORA/releases/latest', {
    headers: { Accept: 'application/vnd.github+json', 'User-Agent': 'VYBORA-Android-Updater' },
    signal: AbortSignal.timeout(10000)
  });
  if (!releaseResponse.ok) {
    return res.status(releaseResponse.status === 404 ? 404 : 502).json({ error: 'Nenhuma versão APK publicada está disponível.' });
  }
  const release = await releaseResponse.json();
  const asset = Array.isArray(release.assets) ? release.assets.find(item => item.name === 'vybora-release.apk') : null;
  const versionName = String(release.tag_name || '').replace(/^v/, '');
  const versionCode = Number(release.name && /^\\d+$/.test(release.name) ? release.name : 0);
  if (!asset || !/^\\d+\\.\\d+\\.\\d+(?:[-+][0-9A-Za-z.-]+)?$/.test(versionName) || !Number.isSafeInteger(versionCode) || versionCode < 1) {
    return res.status(503).json({ error: 'A última release não contém um manifesto de APK válido.' });
  }
  res.set('Cache-Control', 'no-store, max-age=0, must-revalidate');
  res.json({
    schemaVersion: 1,
    appId: 'app.vybora.android',
    versionName,
    versionCode,
    apkUrl: asset.browser_download_url,
    sha256: String(asset.digest || '').replace(/^sha256:/, ''),
    releaseNotes: String(release.body || '').slice(0, 2000),
    publishedAt: release.published_at || null
  });
}));

// Manifesto do OTA próprio. A versão é derivada dos hashes dos arquivos publicados,
// então qualquer alteração em HTML/CSS/JS gera uma nova versão sem depender de fornecedor.
app.get('/ota/manifest.json', asyncRoute(async (_req, res) => {
  const files = ['index.html', 'styles.css', 'app.js'];
  const assets = {};
  for (const name of files) {
    const content = await readFile(path.join(remoteUiDir, name));
    assets[name] = {
      url: '/remote-ui/' + name,
      sha256: createHash('sha256').update(content).digest('hex')
    };
  }
  const version = createHash('sha256').update(JSON.stringify(assets)).digest('hex').slice(0, 16);
  res.set({
    'Cache-Control': 'no-store, max-age=0, must-revalidate',
    'Pragma': 'no-cache',
    'X-Content-Type-Options': 'nosniff'
  });
  res.json({
    schemaVersion: 1,
    appId: 'app.vybora.android',
    channel: 'stable',
    version,
    entrypoint: '/remote-ui/',
    minimumShellVersion: '0.3.0',
    delivery: 'self-hosted-webview',
    updatePolicy: 'check-on-open-and-every-60-seconds',
    assets
  });
}));

// A interface remota é servida pela própria API Railway. Arquivos são revalidados
// a cada acesso para que novas publicações apareçam sem cache antigo.
app.use('/remote-ui', express.static(remoteUiDir, {
  index: 'index.html',
  etag: true,
  maxAge: 0,
  setHeaders(res) {
    res.setHeader('Cache-Control', 'no-cache, max-age=0, must-revalidate');
  }
}));
// Login/cadastro com Google: o ID token é validado pelo endpoint oficial do Google.
app.post('/api/auth/google', asyncRoute(async (req,res) => {
  const idToken = String(req.body?.idToken || req.body?.id_token || req.body?.credential || req.body?.token || '').trim();
  if (!idToken || idToken.length > 10000) return res.status(400).json({ error: 'Credencial Google ausente ou inválida.' });
  const googleClientId = String(process.env.GOOGLE_CLIENT_ID || '').trim();
  if (!googleClientId) {
    console.error('GOOGLE_CLIENT_ID não está configurado no backend.');
    return res.status(503).json({ error: 'Login Google não está configurado no servidor.' });
  }

  let google;
  try {
    const verifyUrl = new URL('https://oauth2.googleapis.com/tokeninfo');
    verifyUrl.searchParams.set('id_token', idToken);
    const response = await fetch(verifyUrl, { signal: AbortSignal.timeout(10000) });
    if (!response.ok) return res.status(401).json({ error: 'A credencial Google expirou ou não é válida.' });
    google = await response.json();
  } catch (error) {
    console.error('Falha ao validar credencial Google:', error?.message || error);
    return res.status(502).json({ error: 'Não foi possível validar a conta Google agora. Tente novamente.' });
  }

  const expiresAt = Number(google?.exp || 0);
  const issuer = String(google?.iss || '');
  if (google?.aud !== googleClientId ||
      expiresAt <= Math.floor(Date.now() / 1000) ||
      !['accounts.google.com', 'https://accounts.google.com'].includes(issuer) ||
      String(google?.email_verified) !== 'true') {
    return res.status(401).json({ error: 'A conta Google não passou na validação de segurança.' });
  }

  const email = String(google.email || '').trim().toLowerCase();
  if (!email || email.length > 254) return res.status(401).json({ error: 'O Google não retornou um e-mail válido.' });

  let { rows } = await pool.query(
    'SELECT id,username,email,display_name,bio,avatar_url,is_verified,created_at FROM users WHERE email=$1',
    [email]
  );
  if (!rows[0]) {
    const displayName = String(google.name || email.split('@')[0]).trim().slice(0,80);
    const base = String(email.split('@')[0]).toLowerCase().replace(/[^a-z0-9_.]/g, '_').replace(/^[_.]+|[_.]+$/g, '').slice(0,16) || 'usuario';
    // A senha aleatória impede que uma conta criada pelo Google tenha uma senha previsível.
    const randomPasswordHash = await bcrypt.hash(randomUUID() + randomUUID(), 12);
    let created = false;
    for (let attempt = 0; attempt < 5 && !created; attempt++) {
      const suffix = randomUUID().replace(/-/g, '').slice(0,6);
      const username = (base.slice(0, 17) + '_' + suffix).slice(0,24);
      try {
        const result = await pool.query(
          'INSERT INTO users(username,email,password_hash,display_name,avatar_url) VALUES($1,$2,$3,$4,$5) RETURNING id,username,email,display_name,bio,avatar_url,is_verified,created_at',
          [username, email, randomPasswordHash, displayName, /^https:\/\//i.test(String(google.picture || '')) ? String(google.picture).slice(0,2000) : null]
        );
        rows = result.rows;
        created = true;
      } catch (error) {
        if (error?.code !== '23505') throw error;
        // Outra tentativa simultânea pode ter criado o e-mail ou ocupado o nome.
        const existing = await pool.query(
          'SELECT id,username,email,display_name,bio,avatar_url,is_verified,created_at FROM users WHERE email=$1',
          [email]
        );
        if (existing.rows[0]) { rows = existing.rows; created = true; }
      }
    }
    if (!created || !rows[0]) return res.status(409).json({ error: 'Não foi possível criar a conta agora. Tente novamente.' });
  }

  const user = rows[0];
  res.json({ token: tokenFor(user), user: safeUser(user) });
}));
app.post('/api/auth/register', asyncRoute(async (req,res) => {
  const username = String(req.body?.username || '').trim().toLowerCase();
  const email = String(req.body?.email || '').trim().toLowerCase();
  const password = String(req.body?.password || '');
  if (!/^[a-z0-9_.]{3,24}$/.test(username)) return res.status(400).json({ error: 'Nome de usuário inválido (3–24 letras, números, ponto ou sublinhado).' });
  if (email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return res.status(400).json({ error: 'E-mail inválido.' });
  if (password.length < 8 || password.length > 72) return res.status(400).json({ error: 'A senha deve ter entre 8 e 72 caracteres.' });
  const hash = await bcrypt.hash(password, 12);
  try {
    const { rows } = await pool.query('INSERT INTO users(username,email,password_hash) VALUES($1,$2,$3) RETURNING id,username,email,display_name,bio,avatar_url,is_verified,created_at', [username,email,hash]);
    const user = rows[0]; res.status(201).json({ token: tokenFor(user), user: safeUser(user) });
  } catch (e) {
    if (e.code === '23505') return res.status(409).json({ error: 'Este e-mail ou nome de usuário já está cadastrado.' });
    throw e;
  }
}));
app.post('/api/auth/login', asyncRoute(async (req,res) => {
  const email = String(req.body?.email || '').trim().toLowerCase(); const password = String(req.body?.password || '');
  const { rows } = await pool.query('SELECT * FROM users WHERE email=$1', [email]);
  if (!rows[0] || !(await bcrypt.compare(password, rows[0].password_hash))) return res.status(401).json({ error: 'E-mail ou senha incorretos.' });
  res.json({ token: tokenFor(rows[0]), user: safeUser(rows[0]) });
}));
app.get('/api/auth/me', auth, (req,res) => res.json({ user: safeUser(req.user) }));
app.patch('/api/users/me', auth, asyncRoute(async (req,res) => {
  const displayName = String(req.body?.displayName ?? req.user.display_name ?? '').trim().slice(0,80);
  const bio = String(req.body?.bio ?? req.user.bio ?? '').trim().slice(0,300);
  const username = String(req.body?.username ?? req.user.username).trim().toLowerCase();
  if (!/^[a-z0-9_.]{3,24}$/.test(username)) return res.status(400).json({ error: 'Nome de usuário inválido (3–24 letras, números, ponto ou sublinhado).' });
  try {
    const { rows } = await pool.query(
      'UPDATE users SET username=$1,display_name=$2,bio=$3,updated_at=NOW() WHERE id=$4 RETURNING id,username,email,display_name,bio,avatar_url,is_verified,created_at',
      [username,displayName,bio,req.user.id]
    );
    res.json({ user: safeUser(rows[0]) });
  } catch (error) {
    if (error?.code === '23505') return res.status(409).json({ error: 'Esse nome de usuário já está em uso.' });
    throw error;
  }
}));
// Busca global de usuários. Deve ficar antes de /api/users/:username para evitar conflito de rota.
app.get('/api/users/search', asyncRoute(async (req,res) => {
  const query = String(req.query.q || '').trim().replace(/^@/, '').toLowerCase();
  if (query.length < 2) return res.json({ users: [] });
  if (query.length > 40) return res.status(400).json({ error: 'A pesquisa deve ter no máximo 40 caracteres.' });
  const escaped = query.replace(/[\\%_]/g, ch => '\\' + ch);
  const { rows } = await pool.query(
    `SELECT id, username, display_name AS "displayName", bio, avatar_url AS "avatarUrl", is_verified AS verified
     FROM users
     WHERE username ILIKE $1 OR COALESCE(display_name, '') ILIKE $1
     ORDER BY CASE WHEN username = $2 THEN 0 WHEN username LIKE $3 THEN 1 ELSE 2 END, username
     LIMIT 30`,
    [`%${escaped}%`, query, `${escaped}%`]
  );
  res.set('Cache-Control', 'no-store');
  res.json({ users: rows });
}));
app.get('/api/search', asyncRoute(async (req,res) => {
  const query = String(req.query.q || '').trim().replace(/^@/, '').toLowerCase();
  const limit = Math.min(30, Math.max(1, Number(req.query.limit) || 10));
  if (query.length < 2) return res.json({ users: [], posts: [] });
  if (query.length > 80) return res.status(400).json({ error: 'A busca é longa demais.' });
  const pattern = '%' + query.replace(/[\\%_]/g, ch => '\\' + ch) + '%';
  const [userResult, postResult] = await Promise.all([
    pool.query(
      `SELECT u.id,u.username,u.display_name AS "displayName",u.avatar_url AS "avatarUrl",u.is_verified AS verified,
        (SELECT COUNT(*)::int FROM follows f WHERE f.following_id=u.id) AS followers
       FROM users u WHERE u.username ILIKE $1 OR u.display_name ILIKE $1
       ORDER BY CASE WHEN lower(u.username)=lower($2) THEN 0 ELSE 1 END,u.username LIMIT $3`,
      [pattern, query, limit]
    ),
    pool.query(
      `SELECT p.id,p.caption,p.media_url AS "mediaUrl",p.media_type AS "mediaType",p.created_at AS "createdAt",
        json_build_object('id',u.id,'username',u.username,'displayName',u.display_name,'avatarUrl',u.avatar_url,'verified',u.is_verified) AS author,
        (SELECT COUNT(*)::int FROM post_likes l WHERE l.post_id=p.id) AS likes,
        (SELECT COUNT(*)::int FROM comments c WHERE c.post_id=p.id) AS comments
       FROM posts p JOIN users u ON u.id=p.user_id
       WHERE p.visibility='public' AND p.caption ILIKE $1
       ORDER BY p.created_at DESC LIMIT $2`,
      [pattern, limit]
    )
  ]);
  res.set('Cache-Control', 'no-store');
  res.json({ users: userResult.rows, posts: postResult.rows });
}));
app.get('/api/users/:username/followers', asyncRoute(async (req,res) => {
  const name = String(req.params.username).toLowerCase();
  const owner = await pool.query('SELECT id FROM users WHERE username=$1',[name]);
  if (!owner.rowCount) return res.status(404).json({ error: 'Usuário não encontrado.' });
  const { rows } = await pool.query(
    `SELECT u.id,u.username,u.display_name AS "displayName",u.avatar_url AS "avatarUrl",u.is_verified AS verified
     FROM follows f JOIN users u ON u.id=f.follower_id
     WHERE f.following_id=$1 ORDER BY f.created_at DESC LIMIT 200`,
    [owner.rows[0].id]
  );
  res.json({ users: rows });
}));
app.get('/api/users/:username/following', asyncRoute(async (req,res) => {
  const name = String(req.params.username).toLowerCase();
  const owner = await pool.query('SELECT id FROM users WHERE username=$1',[name]);
  if (!owner.rowCount) return res.status(404).json({ error: 'Usuário não encontrado.' });
  const { rows } = await pool.query(
    `SELECT u.id,u.username,u.display_name AS "displayName",u.avatar_url AS "avatarUrl",u.is_verified AS verified
     FROM follows f JOIN users u ON u.id=f.following_id
     WHERE f.follower_id=$1 ORDER BY f.created_at DESC LIMIT 200`,
    [owner.rows[0].id]
  );
  res.json({ users: rows });
}));
app.get('/api/users/:username', asyncRoute(async (req,res) => {
  const { rows } = await pool.query(`SELECT u.id,u.username,u.display_name,u.bio,u.avatar_url,u.is_verified,u.created_at,
    (SELECT COUNT(*)::int FROM follows WHERE following_id=u.id) AS followers,
    (SELECT COUNT(*)::int FROM follows WHERE follower_id=u.id) AS following,
    (SELECT COUNT(*)::int FROM posts WHERE user_id=u.id) AS posts_count
    FROM users u WHERE u.username=$1`, [String(req.params.username).toLowerCase()]);
  if (!rows[0]) return res.status(404).json({ error: 'Usuário não encontrado.' }); res.json({ user: rows[0] });
}));
app.post('/api/users/:id/follow', auth, asyncRoute(async (req,res) => {
  const target = req.params.id; if (target === req.user.id) return res.status(400).json({ error: 'Você não pode seguir a si mesmo.' });
  const exists = await pool.query('SELECT id FROM users WHERE id=$1',[target]); if (!exists.rowCount) return res.status(404).json({ error: 'Usuário não encontrado.' });
  const found = await pool.query('SELECT 1 FROM follows WHERE follower_id=$1 AND following_id=$2',[req.user.id,target]);
  if (found.rowCount) { await pool.query('DELETE FROM follows WHERE follower_id=$1 AND following_id=$2',[req.user.id,target]); return res.json({ following: false }); }
  await pool.query('INSERT INTO follows(follower_id,following_id) VALUES($1,$2) ON CONFLICT DO NOTHING',[req.user.id,target]);
  await pool.query("INSERT INTO notifications(recipient_id,actor_id,kind) VALUES($1,$2,'follow')",[target,req.user.id]);
  res.json({ following: true });
}));
app.get('/api/posts', asyncRoute(async (req,res) => {
  const limit = Math.min(Math.max(Number(req.query.limit || 20),1),50); const before = req.query.before ? String(req.query.before) : null;
  const { rows } = await pool.query(`SELECT p.id,p.caption,p.media_url AS "mediaUrl",p.media_type AS "mediaType",p.visibility,p.created_at AS "createdAt",
    json_build_object('id',u.id,'username',u.username,'displayName',u.display_name,'avatarUrl',u.avatar_url,'verified',u.is_verified) AS author,
    (SELECT COUNT(*)::int FROM post_likes l WHERE l.post_id=p.id) AS likes,
    (SELECT COUNT(*)::int FROM comments c WHERE c.post_id=p.id) AS comments
    FROM posts p JOIN users u ON u.id=p.user_id WHERE p.visibility='public' AND ($2::timestamptz IS NULL OR p.created_at<$2)
    ORDER BY p.created_at DESC LIMIT $1`, [limit,before]);
  const posts = await Promise.all(rows.map(async post => ({
    ...post,
    mediaUrl: post.mediaUrl && post.mediaUrl.startsWith('users/') ? await createDownloadUrl(post.mediaUrl) : post.mediaUrl
  })));
  res.set('Cache-Control', 'no-store');
  res.json({ posts });
}));
app.post('/api/posts', auth, asyncRoute(async (req,res) => {
  const caption = String(req.body?.caption || '').trim();
  const mediaFileId = req.body?.mediaFileId ? String(req.body.mediaFileId) : null;
  let mediaUrl = req.body?.mediaUrl ? String(req.body.mediaUrl) : null;
  const mediaType = String(req.body?.mediaType || (mediaFileId || mediaUrl ? 'image' : 'text'));
  if (!caption && !mediaFileId && !mediaUrl) return res.status(400).json({ error: 'Adicione texto ou mídia à publicação.' });
  if (caption.length > 2200) return res.status(400).json({ error: 'A legenda ultrapassa 2200 caracteres.' });
  if (!['text','image','video'].includes(mediaType)) return res.status(400).json({ error: 'Tipo de mídia inválido.' });
  if (mediaFileId) {
    const file = await pool.query("SELECT object_key,content_type FROM user_files WHERE id=$1 AND owner_id=$2 AND category='post_media' AND status='ready' AND deleted_at IS NULL", [mediaFileId, req.user.id]);
    if (!file.rowCount) return res.status(400).json({ error: 'A mídia não foi enviada ou validada para esta conta.' });
    const expectedType = mediaType === 'video' ? 'video/' : 'image/';
    if (!file.rows[0].content_type.startsWith(expectedType)) return res.status(400).json({ error: 'O tipo do arquivo não corresponde à publicação.' });
    mediaUrl = file.rows[0].object_key;
  } else if (mediaUrl && !/^https:\/\//i.test(mediaUrl)) {
    return res.status(400).json({ error: 'A URL da mídia precisa usar HTTPS.' });
  }
  const { rows } = await pool.query('INSERT INTO posts(user_id,caption,media_url,media_type) VALUES($1,$2,$3,$4) RETURNING id,caption,media_url AS "mediaUrl",media_type AS "mediaType",created_at AS "createdAt"',[req.user.id,caption,mediaUrl,mediaType]);
  const post = rows[0];
  if (post.mediaUrl && post.mediaUrl.startsWith('users/')) post.mediaUrl = await createDownloadUrl(post.mediaUrl);
  res.status(201).json({ post: { ...post, author: safeUser(req.user), likes: 0, comments: 0 } });
}));
app.post('/api/posts/:id/like', auth, asyncRoute(async (req,res) => {
  const post = await pool.query('SELECT id,user_id FROM posts WHERE id=$1',[req.params.id]); if (!post.rowCount) return res.status(404).json({ error: 'Publicação não encontrada.' });
  const found = await pool.query('SELECT 1 FROM post_likes WHERE user_id=$1 AND post_id=$2',[req.user.id,req.params.id]);
  if (found.rowCount) { await pool.query('DELETE FROM post_likes WHERE user_id=$1 AND post_id=$2',[req.user.id,req.params.id]); return res.json({ liked: false }); }
  await pool.query('INSERT INTO post_likes(user_id,post_id) VALUES($1,$2) ON CONFLICT DO NOTHING',[req.user.id,req.params.id]);
  if (post.rows[0].user_id !== req.user.id) await pool.query("INSERT INTO notifications(recipient_id,actor_id,kind,entity_id) VALUES($1,$2,'like',$3)",[post.rows[0].user_id,req.user.id,req.params.id]);
  res.json({ liked: true });
}));
app.post('/api/posts/:id/comments', auth, asyncRoute(async (req,res) => {
  const body = String(req.body?.body || '').trim(); if (!body || body.length > 1200) return res.status(400).json({ error: 'O comentário deve ter de 1 a 1200 caracteres.' });
  const post = await pool.query('SELECT user_id FROM posts WHERE id=$1',[req.params.id]); if (!post.rowCount) return res.status(404).json({ error: 'Publicação não encontrada.' });
  const { rows } = await pool.query('INSERT INTO comments(post_id,user_id,body) VALUES($1,$2,$3) RETURNING id,post_id AS "postId",body,created_at AS "createdAt"',[req.params.id,req.user.id,body]);
  if (post.rows[0].user_id !== req.user.id) await pool.query("INSERT INTO notifications(recipient_id,actor_id,kind,entity_id) VALUES($1,$2,'comment',$3)",[post.rows[0].user_id,req.user.id,req.params.id]);
  res.status(201).json({ comment: { ...rows[0], author: safeUser(req.user) } });
}));
app.get('/api/posts/:id/comments', asyncRoute(async (req,res) => {
  const { rows } = await pool.query(`SELECT c.id,c.body,c.created_at AS "createdAt",json_build_object('id',u.id,'username',u.username,'displayName',u.display_name,'avatarUrl',u.avatar_url) AS author FROM comments c JOIN users u ON u.id=c.user_id WHERE c.post_id=$1 ORDER BY c.created_at ASC LIMIT 100`,[req.params.id]); res.json({ comments: rows });
}));
app.get('/api/messages/:username', auth, asyncRoute(async (req,res) => {
  const other = await pool.query('SELECT id FROM users WHERE username=$1',[String(req.params.username).toLowerCase()]); if (!other.rowCount) return res.status(404).json({ error: 'Usuário não encontrado.' });
  const { rows } = await pool.query(`SELECT m.id,m.body,m.created_at AS "createdAt",m.read_at AS "readAt",m.sender_id AS "senderId",m.recipient_id AS "recipientId" FROM messages m WHERE (m.sender_id=$1 AND m.recipient_id=$2) OR (m.sender_id=$2 AND m.recipient_id=$1) ORDER BY m.created_at ASC LIMIT 200`,[req.user.id,other.rows[0].id]);
  await pool.query('UPDATE messages SET read_at=NOW() WHERE sender_id=$1 AND recipient_id=$2 AND read_at IS NULL',[other.rows[0].id,req.user.id]); res.json({ messages: rows });
}));
app.post('/api/messages/:username', auth, asyncRoute(async (req,res) => {
  const body = String(req.body?.body || '').trim(); if (!body || body.length > 5000) return res.status(400).json({ error: 'A mensagem deve ter de 1 a 5000 caracteres.' });
  const recipient = await pool.query('SELECT id FROM users WHERE username=$1',[String(req.params.username).toLowerCase()]); if (!recipient.rowCount) return res.status(404).json({ error: 'Usuário não encontrado.' });
  if (recipient.rows[0].id === req.user.id) return res.status(400).json({ error: 'Não é possível enviar mensagem para si mesmo.' });
  const { rows } = await pool.query('INSERT INTO messages(sender_id,recipient_id,body) VALUES($1,$2,$3) RETURNING id,body,created_at AS "createdAt",sender_id AS "senderId",recipient_id AS "recipientId"',[req.user.id,recipient.rows[0].id,body]);
  await pool.query("INSERT INTO notifications(recipient_id,actor_id,kind,entity_id) VALUES($1,$2,'message',$3)",[recipient.rows[0].id,req.user.id,rows[0].id]); res.status(201).json({ message: rows[0] });
}));
// Dados privados opcionais da própria conta; nunca fazem parte do perfil público.
app.get('/api/users/me/private-profile', auth, asyncRoute(async (req,res) => {
  const { rows } = await pool.query('SELECT birth_date AS "birthDate",phone_e164 AS "phone",country_code AS "countryCode",locale,updated_at AS "updatedAt" FROM user_private_profiles WHERE user_id=$1',[req.user.id]);
  res.json({ privateProfile: rows[0] || { birthDate: null, phone: null, countryCode: null, locale: null } });
}));
app.patch('/api/users/me/private-profile', auth, asyncRoute(async (req,res) => {
  const birthDate = req.body?.birthDate ? String(req.body.birthDate) : null;
  const phone = req.body?.phone ? String(req.body.phone).trim().slice(0,20) : null;
  const countryCode = req.body?.countryCode ? String(req.body.countryCode).trim().toUpperCase() : null;
  const locale = req.body?.locale ? String(req.body.locale).trim().slice(0,20) : null;
  if (birthDate && !/^\d{4}-\d{2}-\d{2}$/.test(birthDate)) return res.status(400).json({ error: 'Data de nascimento inválida; use AAAA-MM-DD.' });
  if (countryCode && !/^[A-Z]{2}$/.test(countryCode)) return res.status(400).json({ error: 'Código do país inválido.' });
  const { rows } = await pool.query(`INSERT INTO user_private_profiles(user_id,birth_date,phone_e164,country_code,locale,updated_at)
    VALUES($1,$2,$3,$4,$5,NOW()) ON CONFLICT(user_id) DO UPDATE SET birth_date=EXCLUDED.birth_date,phone_e164=EXCLUDED.phone_e164,country_code=EXCLUDED.country_code,locale=EXCLUDED.locale,updated_at=NOW()
    RETURNING birth_date AS "birthDate",phone_e164 AS "phone",country_code AS "countryCode",locale,updated_at AS "updatedAt"`,[req.user.id,birthDate,phone,countryCode,locale]);
  res.json({ privateProfile: rows[0] });
}));

// Exportação portátil dos dados da própria conta em JSON (LGPD/data portability).
app.get('/api/users/me/export', auth, asyncRoute(async (req,res) => {
  const id = req.user.id;
  const queries = await Promise.all([
    pool.query('SELECT id,username,email,display_name,bio,avatar_url,is_verified,created_at,updated_at FROM users WHERE id=$1',[id]),
    pool.query('SELECT birth_date,phone_e164,country_code,locale,updated_at FROM user_private_profiles WHERE user_id=$1',[id]),
    pool.query('SELECT id,caption,media_url,media_type,visibility,created_at FROM posts WHERE user_id=$1 ORDER BY created_at',[id]),
    pool.query('SELECT l.post_id,l.created_at FROM post_likes l WHERE l.user_id=$1 ORDER BY l.created_at',[id]),
    pool.query('SELECT f.following_id,f.created_at FROM follows f WHERE f.follower_id=$1 ORDER BY f.created_at',[id]),
    pool.query('SELECT c.id,c.post_id,c.body,c.created_at FROM comments c WHERE c.user_id=$1 ORDER BY c.created_at',[id]),
    pool.query('SELECT id,sender_id,recipient_id,body,read_at,created_at FROM messages WHERE sender_id=$1 OR recipient_id=$1 ORDER BY created_at',[id]),
    pool.query("SELECT id,category,original_name,content_type,size_bytes,created_at FROM user_files WHERE owner_id=$1 AND deleted_at IS NULL AND status='ready' ORDER BY created_at",[id]),
    pool.query('SELECT id,kind,entity_id,read_at,created_at FROM notifications WHERE recipient_id=$1 ORDER BY created_at',[id])
  ]);
  res.set('Content-Disposition', 'attachment; filename="vybora-dados-pessoais.json"');
  res.json({ formatVersion: 1, exportedAt: new Date().toISOString(), profile: queries[0].rows[0], privateProfile: queries[1].rows[0] || null, posts: queries[2].rows, likes: queries[3].rows, following: queries[4].rows, comments: queries[5].rows, messages: queries[6].rows, files: queries[7].rows, notifications: queries[8].rows });
}));

// Uploads use a private bucket, short-lived URLs, declared size limits and an explicit completion check.
app.post('/api/files/upload-url', auth, asyncRoute(async (req,res) => {
  const missingStorage = ['S3_BUCKET','S3_REGION','S3_ACCESS_KEY_ID','S3_SECRET_ACCESS_KEY'].filter(name => !process.env[name]);
  if (missingStorage.length) return res.status(503).json({ error: 'O armazenamento de mídia ainda não foi configurado no servidor. O administrador precisa configurar o bucket e as credenciais de armazenamento.' });
  const originalName = String(req.body?.fileName || '').trim().split(/[\\/]/).pop();
  const contentType = String(req.body?.contentType || '').toLowerCase();
  const category = String(req.body?.category || 'attachment');
  const sizeBytes = Number(req.body?.sizeBytes);
  const allowedTypes = new Set(['image/jpeg','image/png','image/webp','image/gif','video/mp4','video/webm','application/pdf']);
  const maxBytes = category === 'post_media' && contentType.startsWith('video/') ? 100 * 1024 * 1024 : 25 * 1024 * 1024;
  if (!originalName || originalName.length > 255) return res.status(400).json({ error: 'Nome de arquivo inválido.' });
  if (!allowedTypes.has(contentType)) return res.status(415).json({ error: 'Tipo de arquivo não permitido.' });
  if (!['avatar','post_media','document','attachment'].includes(category)) return res.status(400).json({ error: 'Categoria inválida.' });
  if (!Number.isSafeInteger(sizeBytes) || sizeBytes < 1 || sizeBytes > maxBytes) return res.status(413).json({ error: `Arquivo inválido ou maior que o limite de ${Math.floor(maxBytes / 1024 / 1024)} MB.` });
  const key = `users/${req.user.id}/${randomUUID()}`;
  const uploadUrl = await createUploadUrl(key, contentType, sizeBytes);
  const { rows } = await pool.query("INSERT INTO user_files(owner_id,category,original_name,content_type,object_key,expected_size_bytes,status) VALUES($1,$2,$3,$4,$5,$6,'pending') RETURNING id,category,original_name AS \"fileName\",content_type AS \"contentType\",expected_size_bytes AS \"expectedSizeBytes\",status,created_at AS \"createdAt\"",[req.user.id,category,originalName,contentType,key,sizeBytes]);
  res.status(201).json({ file: rows[0], uploadUrl, method: 'PUT', expiresInSeconds: 300, requiredHeaders: { 'Content-Type': contentType, 'Content-Length': String(sizeBytes) }, completionRequired: true });
}));
app.post('/api/files/:id/complete', auth, asyncRoute(async (req,res) => {
  const { rows } = await pool.query('SELECT id,object_key,content_type,expected_size_bytes,status FROM user_files WHERE id=$1 AND owner_id=$2 AND deleted_at IS NULL',[req.params.id,req.user.id]);
  const file = rows[0];
  if (!file) return res.status(404).json({ error: 'Arquivo não encontrado.' });
  if (file.status === 'ready') return res.json({ ok: true, status: 'ready' });
  const object = await inspectObject(file.object_key);
  if (object.sizeBytes !== Number(file.expected_size_bytes) || object.contentType !== file.content_type) {
    return res.status(422).json({ error: 'O arquivo enviado não corresponde ao tamanho ou tipo declarado. Ele não foi liberado para uso.' });
  }
  const updated = await pool.query("UPDATE user_files SET size_bytes=$1,checksum_sha256=$2,object_etag=$3,object_version_id=$4,status='ready',uploaded_at=NOW() WHERE id=$5 AND owner_id=$6 AND status='pending' RETURNING id,category,original_name AS \"fileName\",content_type AS \"contentType\",size_bytes AS \"sizeBytes\",checksum_sha256 AS \"checksumSha256\",status,uploaded_at AS \"uploadedAt\"",[object.sizeBytes,object.checksumSha256,object.etag,object.versionId,file.id,req.user.id]);
  res.json({ ok: true, file: updated.rows[0] || { id: file.id, status: 'ready' } });
}));
app.get('/api/files', auth, asyncRoute(async (req,res) => {
  const { rows } = await pool.query("SELECT id,category,original_name AS \"fileName\",content_type AS \"contentType\",size_bytes AS \"sizeBytes\",status,uploaded_at AS \"uploadedAt\",created_at AS \"createdAt\" FROM user_files WHERE owner_id=$1 AND deleted_at IS NULL AND status='ready' ORDER BY created_at DESC LIMIT 200",[req.user.id]);
  res.json({ files: rows });
}));
app.get('/api/files/:id/download-url', auth, asyncRoute(async (req,res) => {
  const { rows } = await pool.query("SELECT object_key FROM user_files WHERE id=$1 AND owner_id=$2 AND deleted_at IS NULL AND status='ready'",[req.params.id,req.user.id]);
  if (!rows[0]) return res.status(404).json({ error: 'Arquivo não encontrado.' });
  res.json({ downloadUrl: await createDownloadUrl(rows[0].object_key), expiresInSeconds: 120 });
}));
app.delete('/api/files/:id', auth, asyncRoute(async (req,res) => {
  // Soft-delete first; a separate retention job can remove the object after recovery window.
  const result = await pool.query('UPDATE user_files SET deleted_at=NOW() WHERE id=$1 AND owner_id=$2 AND deleted_at IS NULL',[req.params.id,req.user.id]);
  if (!result.rowCount) return res.status(404).json({ error: 'Arquivo não encontrado.' });
  res.status(204).end();
}));
app.post('/api/files/:id/restore', auth, asyncRoute(async (req,res) => {
  // Restore only metadata; object existence is checked before re-enabling access.
  const { rows } = await pool.query("SELECT object_key,content_type,size_bytes FROM user_files WHERE id=$1 AND owner_id=$2 AND deleted_at IS NOT NULL AND status='ready'",[req.params.id,req.user.id]);
  if (!rows[0]) return res.status(404).json({ error: 'Arquivo recuperável não encontrado.' });
  const object = await inspectObject(rows[0].object_key);
  if (object.sizeBytes !== Number(rows[0].size_bytes) || object.contentType !== rows[0].content_type) return res.status(409).json({ error: 'O arquivo armazenado não passou na verificação de integridade.' });
  await pool.query('UPDATE user_files SET deleted_at=NULL WHERE id=$1 AND owner_id=$2',[req.params.id,req.user.id]);
  res.json({ ok: true, restored: true });
}));

app.get('/api/notifications', auth, asyncRoute(async (req,res) => {
  const { rows } = await pool.query(`SELECT n.id,n.kind,n.entity_id AS "entityId",n.read_at AS "readAt",n.created_at AS "createdAt",json_build_object('username',u.username,'displayName',u.display_name,'avatarUrl',u.avatar_url) AS actor FROM notifications n LEFT JOIN users u ON u.id=n.actor_id WHERE n.recipient_id=$1 ORDER BY n.created_at DESC LIMIT 100`,[req.user.id]); res.json({ notifications: rows });
}));
app.post('/api/notifications/read', auth, asyncRoute(async (req,res) => { await pool.query('UPDATE notifications SET read_at=NOW() WHERE recipient_id=$1 AND read_at IS NULL',[req.user.id]); res.json({ ok: true }); }));
app.use((req,res) => res.status(404).json({ error: 'Rota não encontrada.' }));
app.use((err,req,res,_next) => { console.error('API error:', err?.message || err); res.status(500).json({ error: process.env.NODE_ENV === 'production' ? 'Erro interno do servidor.' : (err?.message || 'Erro interno.') }); });
// Banco atualizado explicitamente com `npm run migrate` durante o deploy;
// a API não altera o schema automaticamente ao iniciar.

app.listen(port, '0.0.0.0', () => console.log(`VYBORA API listening on ${port}`));
process.on('SIGTERM', async () => { await pool.end(); process.exit(0); });
process.on('SIGINT', async () => { await pool.end(); process.exit(0); });
