-- Seed: primeiro usuario OWNER para acesso inicial
-- Senha: admin123 (BCrypt hash)
-- TROQUE A SENHA APOS O PRIMEIRO LOGIN via endpoint PATCH /users/{id}
INSERT INTO users (id, name, email, password_hash, role, active, created_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Admin',
    'admin@agenciahub.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'OWNER',
    true,
    NOW()
);
