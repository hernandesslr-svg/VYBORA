-- Perfis exclusivamente demonstrativos para validar feed e páginas de perfil.
-- Credenciais intencionalmente inválidas: estas contas não podem autenticar.
INSERT INTO users (username, email, password_hash, display_name, bio, avatar_url, is_verified)
VALUES
 ('aline_demo', 'aline.demo@example.invalid', 'DEMO_ONLY_NO_LOGIN', 'Aline', 'Lifestyle • rotina • fotos 💜 Perfil de demonstração.', 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=600&q=88', FALSE),
 ('yasmin_demo', 'yasmin.demo@example.invalid', 'DEMO_ONLY_NO_LOGIN', 'Yasmin', 'Moda, cafés e momentos especiais ✨ Perfil de demonstração.', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=600&q=88', FALSE),
 ('larissa_demo', 'larissa.demo@example.invalid', 'DEMO_ONLY_NO_LOGIN', 'Larissa', 'Viagens, música e boas vibrações 🌴 Perfil de demonstração.', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=600&q=88', FALSE),
 ('beatriz_demo', 'beatriz.demo@example.invalid', 'DEMO_ONLY_NO_LOGIN', 'Beatriz', 'Looks, beleza e dia a dia 🌸 Perfil de demonstração.', 'https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?auto=format&fit=crop&w=600&q=88', FALSE)
ON CONFLICT (username) DO UPDATE SET
 display_name = EXCLUDED.display_name,
 bio = EXCLUDED.bio,
 avatar_url = EXCLUDED.avatar_url,
 is_verified = FALSE,
 updated_at = NOW();

-- Publicações separadas (uma imagem por publicação), sem mosaicos nem imagens recortadas.
WITH demo_posts(username, caption, media_url, age_hours) AS (
 VALUES
 ('aline_demo', 'Um momento só meu 💗', 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=1200&q=90', 1),
 ('aline_demo', 'No meu cantinho favorito ✨', 'https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=1200&q=90', 5),
 ('aline_demo', 'Look confortável para hoje 🤍', 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=1200&q=90', 9),
 ('aline_demo', 'Luz bonita no fim da tarde 🌅', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=1200&q=90', 14),
 ('aline_demo', 'Pequenas coisas, bons dias 💜', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=1200&q=90', 20),
 ('aline_demo', 'Mais um registro para guardar 📸', 'https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?auto=format&fit=crop&w=1200&q=90', 28),
 ('yasmin_demo', 'Um passeio sem pressa ☀️', 'https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=1200&q=88', 2),
 ('yasmin_demo', 'Detalhes que fazem diferença ✨', 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=1200&q=88', 8),
 ('yasmin_demo', 'Domingo de boas 💕', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=1200&q=88', 18),
 ('larissa_demo', 'Colecionando lugares 🌴', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=1200&q=88', 3),
 ('larissa_demo', 'Meu look de hoje 🖤', 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=1200&q=88', 11),
 ('larissa_demo', 'Fim de tarde perfeito 🌇', 'https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?auto=format&fit=crop&w=1200&q=88', 21),
 ('beatriz_demo', 'Oi, feed! 🌸', 'https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?auto=format&fit=crop&w=1200&q=88', 4),
 ('beatriz_demo', 'Uma pausa na correria ☕', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=1200&q=88', 12),
 ('beatriz_demo', 'Um pouco de cor no dia 💗', 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=1200&q=88', 24)
)
INSERT INTO posts (user_id, caption, media_url, media_type, visibility, created_at)
SELECT u.id, d.caption, d.media_url, 'image', 'public', NOW() - (d.age_hours || ' hours')::interval
FROM demo_posts d
JOIN users u ON u.username = d.username
WHERE NOT EXISTS (
 SELECT 1 FROM posts p
 WHERE p.user_id = u.id AND p.caption = d.caption AND p.media_url = d.media_url
);
