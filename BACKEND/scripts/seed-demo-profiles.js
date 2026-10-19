import pg from 'pg';
import bcrypt from 'bcryptjs';
import { randomUUID } from 'node:crypto';
import dotenv from 'dotenv';

dotenv.config();
if (process.env.ENABLE_DEMO_SEED !== 'true') {
  console.error('Bloqueado: defina ENABLE_DEMO_SEED=true apenas no ambiente de teste para executar.');
  process.exit(1);
}
if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL não configurada.');

const { Pool } = pg;
const pool = new Pool({ connectionString: process.env.DATABASE_URL, ssl: process.env.DATABASE_SSL === 'true' ? { rejectUnauthorized: false } : undefined });
const profiles = [
  {
    username: 'ana.rose.demo', email: 'ana.rose.demo@example.invalid', name: 'Ana Rose',
    bio: 'Perfil demonstrativo do VYBORA ✨ Lifestyle, games, viagens e momentos do dia a dia. Conta de teste.',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=480&h=480&fit=crop&crop=faces',
    cover: 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=1400&h=520&fit=crop',
    posts: [
      ['Fim de tarde com uma vista inesquecível 🌅', 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&auto=format&fit=crop', 'image'],
      ['Um pouco de cor para alegrar o dia 💜', 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=900&auto=format&fit=crop', 'image'],
      ['Meu lugar favorito para respirar fundo 🌴', 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=900&auto=format&fit=crop', 'image'],
      ['Luzes da cidade e boas lembranças ✨', 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=900&auto=format&fit=crop', 'image'],
      ['Um look que combina com meu humor hoje 🖤', 'https://images.unsplash.com/photo-1529139574466-a303027c1d8b?w=900&auto=format&fit=crop', 'image'],
      ['Pequenas pausas, grandes energias ☕', 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&auto=format&fit=crop', 'image'],
      ['Dias de sol e alma leve ☀️', 'https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?w=900&auto=format&fit=crop', 'image'],
      ['Um cantinho cheio de personalidade 🎮', 'https://images.unsplash.com/photo-1593305841991-05c297ba4575?w=900&auto=format&fit=crop', 'image'],
      ['Flores porque sim 🌸', 'https://images.unsplash.com/photo-1490750967868-88aa4486c946?w=900&auto=format&fit=crop', 'image'],
      ['Respirando novos ares ✨', 'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=900&auto=format&fit=crop', 'image'],
      ['A cidade fica diferente à noite 🌃', 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=900&auto=format&fit=crop', 'image'],
      ['Guardando momentos bonitos 📸', 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=900&auto=format&fit=crop', 'image']
    ]
  },
  {
    username: 'leo.viagens.demo', email: 'leo.viagens.demo@example.invalid', name: 'Leo Viagens',
    bio: 'Explorando lugares novos e compartilhando cada descoberta. Perfil de demonstração.',
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=480&h=480&fit=crop&crop=faces',
    cover: 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1400&h=520&fit=crop',
    posts: [
      ['Montanhas, silêncio e ar puro 🏔️', 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=900&auto=format&fit=crop', 'image'],
      ['A estrada também faz parte da aventura', 'https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=900&auto=format&fit=crop', 'image'],
      ['Um destino para guardar na memória', 'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=900&auto=format&fit=crop', 'image']
    ]
  },
  {
    username: 'mila.fotografia.demo', email: 'mila.fotografia.demo@example.invalid', name: 'Mila Fotografia',
    bio: 'Fotografia, detalhes e pequenos momentos do cotidiano. Conta fictícia para testes.',
    avatar: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=480&h=480&fit=crop&crop=faces',
    cover: 'https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?w=1400&h=520&fit=crop',
    posts: [
      ['A luz da manhã muda tudo ☀️', 'https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?w=900&auto=format&fit=crop', 'image'],
      ['Flores e cores que inspiram', 'https://images.unsplash.com/photo-1490750967868-88aa4486c946?w=900&auto=format&fit=crop', 'image'],
      ['Texturas, sombras e composição', 'https://images.unsplash.com/photo-1494438639946-1ebd1d20bf85?w=900&auto=format&fit=crop', 'image']
    ]
  },
  {
    username: 'kai.music.demo', email: 'kai.music.demo@example.invalid', name: 'Kai Music',
    bio: 'Música ao vivo, bastidores e energia boa 🎧 Perfil fictício para validar publicações.',
    avatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=480&h=480&fit=crop&crop=faces',
    cover: 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1400&h=520&fit=crop',
    posts: [
      ['A energia de um show ao vivo 🎶', 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=900&auto=format&fit=crop', 'image'],
      ['Noite de música e amigos', 'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=900&auto=format&fit=crop', 'image'],
      ['Antes de subir ao palco 🎤', 'https://images.unsplash.com/photo-1506157786151-b8491531f063?w=900&auto=format&fit=crop', 'image']
    ]
  }
];

try {
  await pool.query('BEGIN');
  const demoIds = [];
  for (const profile of profiles) {
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
    demoIds.push(user.id);
    for (const [caption, mediaUrl, mediaType] of profile.posts) {
      await pool.query(
        `INSERT INTO posts(user_id,caption,media_url,media_type,visibility)
         SELECT $1,$2,$3,$4,'public'
         WHERE NOT EXISTS (SELECT 1 FROM posts WHERE user_id=$1 AND caption=$2)`,
        [user.id, caption, mediaUrl, mediaType]
      );
    }
  }
  await pool.query('COMMIT');
  console.log(`Perfis de demonstração prontos: ${profiles.length}; publicações cadastradas: ${profiles.reduce((n,p)=>n+p.posts.length,0)} (execução idempotente).`);
} catch (error) {
  await pool.query('ROLLBACK');
  throw error;
} finally {
  await pool.end();
}
