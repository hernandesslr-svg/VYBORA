import pg from 'pg';
import bcrypt from 'bcryptjs';
import { randomUUID } from 'node:crypto';
import dotenv from 'dotenv';

dotenv.config();
if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL não configurada.');

const { Pool } = pg;
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.DATABASE_SSL === 'true' ? { rejectUnauthorized: false } : undefined
});

const profile = {
  username: 'ana_rose',
  email: 'ana.rose.demo@example.invalid',
  name: 'Ana Rose',
  bio: '✨ Lifestyle, games, viagens e pequenos momentos do dia a dia. Bem-vindos ao meu cantinho 💜',
  avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=480&h=480&fit=crop&crop=faces',
  cover: 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=1400&h=520&fit=crop',
  posts: [
    ['Um pouco de mim por aqui 💜', 'https://images.unsplash.com/photo-1529139574466-a303027c1d8b?w=1000&auto=format&fit=crop', 'image'],
    ['Look do dia ✨', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=1000&auto=format&fit=crop&crop=faces', 'image'],
    ['Dias leves e boas energias ☀️', 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=1000&auto=format&fit=crop&crop=faces', 'image'],
    ['Um fim de tarde perfeito 🌅', 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1000&auto=format&fit=crop', 'image'],
    ['Meu lugar favorito para relaxar 🌴', 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1000&auto=format&fit=crop', 'image'],
    ['Um café e um tempo pra mim ☕', 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1000&auto=format&fit=crop', 'image'],
    ['Noite iluminada na cidade 🌃', 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=1000&auto=format&fit=crop', 'image'],
    ['Cantinho gamer 🎮', 'https://images.unsplash.com/photo-1593305841991-05c297ba4575?w=1000&auto=format&fit=crop', 'image'],
    ['Flores que deixam o dia mais bonito 🌸', 'https://images.unsplash.com/photo-1490750967868-88aa4486c946?w=1000&auto=format&fit=crop', 'image'],
    ['Uma paisagem para guardar na memória 🏞️', 'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=1000&auto=format&fit=crop', 'image'],
    ['Sol, tranquilidade e boas lembranças 🌞', 'https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?w=1000&auto=format&fit=crop', 'image'],
    ['Mais um momento especial 📸', 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=1000&auto=format&fit=crop', 'image']
  ]
};

try {
  await pool.query('BEGIN');
  const passwordHash = await bcrypt.hash(randomUUID() + randomUUID(), 12);
  const result = await pool.query(
    `INSERT INTO users(username,email,password_hash,display_name,bio,avatar_url,cover_url,is_verified)
     VALUES($1,$2,$3,$4,$5,$6,$7,FALSE)
     ON CONFLICT(username) DO UPDATE SET display_name=EXCLUDED.display_name,bio=EXCLUDED.bio,
       avatar_url=EXCLUDED.avatar_url,cover_url=EXCLUDED.cover_url,updated_at=NOW()
     RETURNING id,username`,
    [profile.username, profile.email, passwordHash, profile.name, profile.bio, profile.avatar, profile.cover]
  );
  const user = result.rows[0];
  for (const [caption, mediaUrl, mediaType] of profile.posts) {
    await pool.query(
      `INSERT INTO posts(user_id,caption,media_url,media_type,visibility)
       SELECT $1,$2,$3,$4,'public'
       WHERE NOT EXISTS (SELECT 1 FROM posts WHERE user_id=$1 AND caption=$2)`,
      [user.id, caption, mediaUrl, mediaType]
    );
  }
  await pool.query('COMMIT');
  const count = await pool.query('SELECT COUNT(*)::int AS total FROM posts WHERE user_id=$1', [user.id]);
  console.log(`Perfil Ana Rose pronto: @${user.username}; publicações: ${count.rows[0].total}.`);
} catch (error) {
  await pool.query('ROLLBACK');
  throw error;
} finally {
  await pool.end();
}
