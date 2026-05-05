-- Seed: primeiro usuario OWNER para acesso inicial
-- Senha: admin123 (BCrypt hash gerado pelo Spring Boot)
-- TROQUE A SENHA APOS O PRIMEIRO LOGIN via endpoint PATCH /users/{id}
INSERT INTO users (id, name, email, password_hash, role, active, created_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Admin',
    'admin@agenciahub.com',
    '$2a$10$Wh.FCi.5ZezD2fR/c7i5N.imfa7ltpN1NG8TCWNYmmsFXwnXjhUpe',
    'OWNER',
    true,
    NOW()
);
