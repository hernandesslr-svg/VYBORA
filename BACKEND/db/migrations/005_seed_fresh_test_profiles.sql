-- Perfis e publicações de teste para validar o feed visualmente.
-- São contas sintéticas claramente identificadas; nenhuma conta real é alterada.
-- Reexecutável: atualiza os perfis de teste e evita duplicar publicações, curtidas e comentários.

INSERT INTO users (username, email, password_hash, display_name, bio, avatar_url, cover_url, is_verified)
VALUES
  ('maya.luz',       'maya.luz@vybora.test',       '$2b$12$seedOnlyNoLoginAccount000000000000000000000000000000000', 'Maya Luz',       'Fotografia, café e pequenos momentos ☀️', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&h=400&q=85', 'https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=1200&h=500&q=85', false),
  ('enzo.rocha',     'enzo.rocha@vybora.test',     '$2b$12$seedOnlyNoLoginAccount000000000000000000000000000000000', 'Enzo Rocha',     'Corrida, trilha e céu aberto 🏃', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&h=400&q=85', 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&h=500&q=85', false),
  ('luna.martins',   'luna.martins@vybora.test',   '$2b$12$seedOnlyNoLoginAccount000000000000000000000000000000000', 'Luna Martins',   'Looks, arte e dias coloridos 🎨', 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=400&h=400&q=85', 'https://images.unsplash.com/photo-1490730141103-6cac27aaab94?auto=format&fit=crop&w=1200&h=500&q=85', false),
  ('caio.nunes',     'caio.nunes@vybora.test',     '$2b$12$seedOnlyNoLoginAccount000000000000000000000000000000000', 'Caio Nunes',     'Música, amigos e rolês 🎧', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=400&h=400&q=85', 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?auto=format&fit=crop&w=1200&h=500&q=85', false),
  ('bia.freitas',    'bia.freitas@vybora.test',    '$2b$12$seedOnlyNoLoginAccount000000000000000000000000000000000', 'Bia Freitas',    'Comida boa e viagens sem roteiro 🍓', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=400&h=400&q=85', 'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?auto=format&fit=crop&w=1200&h=500&q=85', false),
  ('davi.costa',     'davi.costa@vybora.test',     '$2b$12$seedOnlyNoLoginAccount000000000000000000000000000000000', 'Davi Costa',     'Mar, bike e vida leve 🌊', 'https://images.unsplash.com/photo-1504593811423-6dd665756598?auto=format&fit=crop&w=400&h=400&q=85', 'https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=1200&h=500&q=85', false)
ON CONFLICT (username) DO UPDATE SET
  display_name=EXCLUDED.display_name, bio=EXCLUDED.bio, avatar_url=EXCLUDED.avatar_url,
  cover_url=EXCLUDED.cover_url, updated_at=NOW();

WITH seed_posts(username, caption, media_url, media_type, minutes_ago) AS (
  VALUES
    ('maya.luz', 'Luz de fim de tarde e zero pressa 🌅 Qual foi a melhor parte do seu dia?', 'https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?auto=format&fit=crop&w=1200&h=1500&q=85', 'image', 7),
    ('enzo.rocha', 'Saí só pra dar uma volta e encontrei esse visual. A natureza nunca decepciona 🌿', 'https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&w=1200&h=1500&q=85', 'image', 14),
    ('luna.martins', 'Um look confortável, minha playlist favorita e bora viver 💜', 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=1200&h=1500&q=85', 'image', 21),
    ('caio.nunes', 'Som alto no fone, cidade acesa e a noite só começando 🎶', 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?auto=format&fit=crop&w=1200&h=1500&q=85', 'image', 28),
    ('bia.freitas', 'Pequena pausa pra comer bem e agradecer pelas coisas simples 🍓', 'https://images.unsplash.com/photo-1490645935967-10de6ba17061?auto=format&fit=crop&w=1200&h=1500&q=85', 'image', 35),
    ('davi.costa', 'Quando o mar fica assim, eu esqueço até de olhar o celular 🌊', 'https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=1200&h=1500&q=85', 'image', 42),
    ('maya.luz', 'Mais um cantinho bonito guardado na memória 📸', 'https://images.unsplash.com/photo-1500534623283-312aade485b7?auto=format&fit=crop&w=1200&h=1500&q=85', 'image', 49),
    ('enzo.rocha', 'Meta da semana: mexer o corpo, respirar fundo e continuar. Quem vem? 💪', 'https://images.unsplash.com/photo-1551632811-561732d1e306?auto=format&fit=crop&w=1200&h=1500&q=85', 'image', 56),
    ('luna.martins', 'Cores que melhoram qualquer segunda-feira 🎨', 'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?auto=format&fit=crop&w=1200&h=1500&q=85', 'image', 63),
    ('bia.freitas', 'Roteiro perfeito: estrada, janela aberta e nenhuma pressa 🚗', 'https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=1200&h=1500&q=85', 'image', 70)
),
created AS (
  INSERT INTO posts (user_id, caption, media_url, media_type, visibility, created_at)
  SELECT u.id, s.caption, s.media_url, s.media_type, 'public', NOW() - make_interval(mins => s.minutes_ago)
  FROM seed_posts s JOIN users u ON u.username=s.username
  WHERE NOT EXISTS (
    SELECT 1 FROM posts p WHERE p.user_id=u.id AND p.caption=s.caption
  )
  RETURNING id, user_id
)
SELECT COUNT(*) FROM created;

-- Cada perfil de teste curte publicações dos demais perfis.
INSERT INTO post_likes (user_id, post_id)
SELECT actor.id, p.id
FROM users actor
JOIN posts p ON p.user_id <> actor.id
JOIN users owner ON owner.id=p.user_id
WHERE actor.username IN ('maya.luz','enzo.rocha','luna.martins','caio.nunes','bia.freitas','davi.costa')
  AND owner.username IN ('maya.luz','enzo.rocha','luna.martins','caio.nunes','bia.freitas','davi.costa')
  AND p.caption IN (
    'Luz de fim de tarde e zero pressa 🌅 Qual foi a melhor parte do seu dia?',
    'Saí só pra dar uma volta e encontrei esse visual. A natureza nunca decepciona 🌿',
    'Um look confortável, minha playlist favorita e bora viver 💜',
    'Som alto no fone, cidade acesa e a noite só começando 🎶',
    'Pequena pausa pra comer bem e agradecer pelas coisas simples 🍓',
    'Quando o mar fica assim, eu esqueço até de olhar o celular 🌊',
    'Mais um cantinho bonito guardado na memória 📸',
    'Meta da semana: mexer o corpo, respirar fundo e continuar. Quem vem? 💪',
    'Cores que melhoram qualquer segunda-feira 🎨',
    'Roteiro perfeito: estrada, janela aberta e nenhuma pressa 🚗'
  )
ON CONFLICT DO NOTHING;

INSERT INTO comments (post_id, user_id, body, created_at)
SELECT p.id, actor.id, c.body, NOW() - make_interval(mins => c.minutes_ago)
FROM (VALUES
  ('Luz de fim de tarde e zero pressa 🌅 Qual foi a melhor parte do seu dia?', 'enzo.rocha', 'Essa luz ficou absurda! 🔥', 6),
  ('Luz de fim de tarde e zero pressa 🌅 Qual foi a melhor parte do seu dia?', 'bia.freitas', 'Precisava de uma pausa dessas hoje ☀️', 5),
  ('Saí só pra dar uma volta e encontrei esse visual. A natureza nunca decepciona 🌿', 'davi.costa', 'Já quero conhecer esse lugar!', 12),
  ('Saí só pra dar uma volta e encontrei esse visual. A natureza nunca decepciona 🌿', 'maya.luz', 'Ar puro muda o dia inteiro 🌱', 10),
  ('Um look confortável, minha playlist favorita e bora viver 💜', 'bia.freitas', 'A combinação perfeita 😍', 18),
  ('Um look confortável, minha playlist favorita e bora viver 💜', 'caio.nunes', 'Estilo demais!', 16),
  ('Som alto no fone, cidade acesa e a noite só começando 🎶', 'luna.martins', 'Qual é o nome da música? 👀', 25),
  ('Pequena pausa pra comer bem e agradecer pelas coisas simples 🍓', 'maya.luz', 'Agora fiquei com fome 😂', 32),
  ('Quando o mar fica assim, eu esqueço até de olhar o celular 🌊', 'enzo.rocha', 'Paz em forma de foto.', 39),
  ('Mais um cantinho bonito guardado na memória 📸', 'luna.martins', 'Essa merece virar papel de parede!', 46),
  ('Meta da semana: mexer o corpo, respirar fundo e continuar. Quem vem? 💪', 'davi.costa', 'Tô nessa! Vamos com tudo 💪', 53),
  ('Cores que melhoram qualquer segunda-feira 🎨', 'maya.luz', 'A paleta tá linda 💜', 60),
  ('Roteiro perfeito: estrada, janela aberta e nenhuma pressa 🚗', 'caio.nunes', 'Playlist e estrada, fechou!', 67)
) AS c(caption, actor_username, body, minutes_ago)
JOIN posts p ON p.caption=c.caption
JOIN users actor ON actor.username=c.actor_username
WHERE NOT EXISTS (
  SELECT 1 FROM comments old
  WHERE old.post_id=p.id AND old.user_id=actor.id AND old.body=c.body
);
